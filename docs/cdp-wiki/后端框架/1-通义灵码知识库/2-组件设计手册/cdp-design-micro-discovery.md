# CDP 服务注册与发现模块设计手册

> 对应使用手册：[cdp-module-micro-discovery.md](../3-组件使用手册/cdp-module-micro-discovery.md)

## 一、设计目标与背景

CDP 微服务架构中，服务间通信依赖于服务注册与发现机制。该模块基于 Nacos Discovery 和 OpenFeign 构建，为微服务提供自动注册、服务发现和声明式远程调用能力。

设计目标如下：

1. **统一服务间通信** -- 封装 Feign 请求拦截器，自动完成 Token 透传、Trace ID 传递和微服务间鉴权签名，使业务开发者无需关心跨服务调用的认证细节。
2. **安全的微服务间调用** -- 引入 MICRO_TOKEN 机制，在 Feign 调用时对 Token 进行签名转发，防止外部伪造微服务内部调用请求。无可用 Token 时自动生成临时 Token。
3. **负载均衡内置** -- 集成 Spring Cloud LoadBalancer，对 `lb://` 协议的服务调用自动进行客户端负载均衡。同时提供预配置的 `@LoadBalanced` RestTemplate 供非 Feign 场景使用。

## 二、整体架构

模块包结构如下：

```
com.leatop.cdp.micro.discovery
├── CdpMicroDiscoveryAutoConfiguration     # 自动配置入口，扫描包内所有组件
└── config/
    └── FeignConfiguration                 # Feign 拦截器、日志、RestTemplate 配置
```

依赖关系：

```
leatop-cdp-micro-discovery
├── leatop-cdp-micro-core                  # 微服务核心公共模块
├── spring-cloud-starter-alibaba-nacos-discovery   # Nacos 服务注册发现
├── spring-cloud-loadbalancer              # 客户端负载均衡
├── feign-core / feign-slf4j               # OpenFeign 核心与日志
```

Feign 调用的请求头注入链路：

```
业务调用 FeignClient 方法
  -> RequestInterceptor 拦截
    -> 从 CdpTokenHolder / HttpServletRequest 获取当前 Token
    -> 若无可用 Token，调用 SaJwtUtil.createToken 生成临时 Token
    -> 注入 MICRO_TOKEN 头（签名 Token）、原始 Token 头、REQUEST_ID 头
  -> 负载均衡选择目标实例
  -> 发起 HTTP 请求
```

## 三、关键类说明

| 类名 | 角色 | 核心职责 |
|------|------|---------|
| `CdpMicroDiscoveryAutoConfiguration` | 自动配置 | 通过 `@ComponentScan` 扫描 discovery 包下的所有组件，触发 FeignConfiguration 等配置类加载 |
| `FeignConfiguration` | Feign 配置中心 | 注册 RequestInterceptor、Feign 日志级别、RestTemplate 及 RestTemplateCustomizer |

FeignConfiguration 内部注册的核心 Bean：

- **RequestInterceptor** -- Feign 请求拦截器，是模块的核心逻辑。拦截每一次 Feign 调用，执行以下操作：(1) 从 CdpTokenHolder 获取当前线程绑定的 Token；(2) 若不存在，尝试从 HttpServletRequest 的请求头中读取；(3) 若仍不存在（如定时任务、消息消费等非 HTTP 上下文场景），调用 `SaJwtUtil.createToken` 生成有效期 60 秒的临时 Token；(4) 将 Token 同时写入 MICRO_TOKEN 和原始 Token Name 两个请求头；(5) 注入 Trace ID 请求头，保证跨服务链路追踪连续。

- **RestTemplateCustomizer** -- 为 RestTemplate 添加两个拦截器：一是 Token 注入拦截器（注入 MICRO_TOKEN），二是 LoadBalancerInterceptor（负载均衡）。仅在 LoadBalancerInterceptor Bean 存在时才注册（`@ConditionalOnBean`）。

- **RestTemplate** -- 使用 `@LoadBalanced` 注解标注，使其支持 `http://service-name/path` 形式的服务间调用。通过 `@ConditionalOnMissingBean` 确保业务侧可自定义。

- **Feign Logger** -- 默认设置为 `FULL` 级别（记录完整请求和响应），使用 Slf4jLogger 输出。生产环境可通过配置降低日志级别。

> 设计决策：FeignConfiguration 实现了 EnvironmentAware 和 InitializingBean 接口，在 `afterPropertiesSet` 中动态读取 SA-Token 配置的 Token Name（`sa-token.token-name`），默认为 `cdp-token`。这种延迟初始化方式避免了在字段注入时 SA-Token 尚未完成配置加载的时序问题。请求头白名单（`includeHeaders`）也在此阶段组装，默认包含 `xxl-job-access-token`、MICRO_TOKEN、RESOURCE_ID、REQUEST_ID 和 Token Name。

## 四、扩展机制

### 4.1 自定义请求头透传

通过配置 `cdp.feign.includeHeaders` 属性（逗号分隔），可扩展 Feign 调用时需要透传的请求头列表。框架默认透传 XXL-Job 访问令牌和 CDP 内部通信头，业务侧可按需添加自定义头。

### 4.2 自定义临时 Token 配置

`cdp.feign.token.temp-user` 控制临时 Token 对应的用户标识（默认 `cdp-temp-user`），`cdp.feign.token.timeout` 控制临时 Token 有效期（默认 60 秒）。在无 HTTP 上下文的场景（如定时任务触发的跨服务调用）中，这些配置决定了临时身份的特征。

### 4.3 替换负载均衡策略

模块引入的 `spring-cloud-loadbalancer` 默认使用轮询策略。可通过 Spring Cloud LoadBalancer 的标准扩展机制（实现 `ReactorServiceInstanceLoadBalancer` 接口并注册为 Bean）替换为随机、加权或自定义策略。

### 4.4 Feign 日志级别调整

FeignConfiguration 中默认配置为 `Logger.Level.FULL`。生产环境建议通过配置 `feign.client.config.default.loggerLevel=BASIC` 降低日志输出量，仅记录请求方法、URL 和响应状态码。
