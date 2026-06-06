# CDP 服务网关模块设计手册

> 对应使用手册：[cdp-module-micro-gateway.md](../3-组件使用手册/cdp-module-micro-gateway.md)

## 一、设计目标与背景

在 CDP 微服务架构中，网关是所有外部流量的唯一入口。它承担统一路由转发、认证鉴权、链路追踪、限流保护和跨域处理等横切关注点，使后端业务微服务只需专注于自身业务逻辑。

设计目标如下：

1. **响应式架构** -- 基于 Spring Cloud Gateway（WebFlux）构建，采用非阻塞 I/O 模型，适应高并发网关场景。网关项目不能引入 `spring-boot-starter-web`，两者互斥。
2. **认证前置** -- 在网关层统一完成 SA-Token 认证和权限校验，后端服务通过请求头中的 Token 信息获取用户身份，无需重复鉴权。
3. **限流集成** -- 深度集成 Sentinel Gateway Adapter，在路由层面实现 QPS 限流和 API 分组限流，并将规则持久化到本地文件，重启不丢失。
4. **Ant 风格路径兼容** -- Spring Cloud Gateway 默认使用 PathPattern 进行路径匹配，不支持 `/**/*.css` 这类 Ant 风格的通配符。CDP 通过自定义 Predicate 补充了 Ant 风格路径匹配能力。
5. **零侵入默认配置** -- 通过 ApplicationListener 在环境准备阶段注入 Sentinel 限流失败的默认响应体和 Jasypt 加密器配置，开发者无需额外配置即可获得合理的默认行为。

## 二、整体架构

模块包结构如下：

```
com.leatop.cdp.micro.gateway
├── GatewayAutoConfiguration                        # 自动配置入口，注册核心 Filter 和 SA-Token 认证
├── auth/
│   └── CdpFilterAuthStrategy                       # SA-Token 认证策略实现
├── config/
│   └── SentinelGatewayRuleConfig                   # Sentinel 网关限流规则初始化与本地持久化
├── listener/
│   └── CdpGatewayDefaultConfigListener             # 应用环境准备阶段注入默认配置
├── predicate/
│   └── AntPathRoutePredicateFactory                # 自定义 Ant 风格路由断言
├── common/
│   ├── LocalSaveApiDefinitionChangeObserver         # API 分组定义变更时本地持久化
│   └── PrintFinishApplicationRunner                 # 启动完成后打印服务地址
└── spring/
    └── CdpTokenContextForSpringReactor              # SA-Token Reactor 上下文扩展（当前已注释）
```

请求处理的完整链路为：

```
外部请求 -> traceIdFilter（生成/传递 Trace ID）
         -> SentinelGatewayFilter（限流检查）
         -> SaReactorFilter（SA-Token 认证鉴权）
         -> 路由断言匹配（Path / AntPath）
         -> 路由过滤器（StripPrefix 等）
         -> 负载均衡转发至后端服务（lb://service-name）
```

## 三、核心设计模式

### 3.1 Filter Chain 模式：分层过滤器链

GatewayAutoConfiguration 注册了三个关键过滤器，通过 `@Order` 控制执行顺序：

- **traceIdFilter**（`Ordered.HIGHEST_PRECEDENCE`）-- 最先执行。从请求头读取 `REQUEST_ID`，若不存在则通过 `IdUtils.nanoId(12)` 生成 12 位 NanoID。将其写入 MDC 用于日志关联，并通过 `exchange.mutate()` 将 Trace ID 注入下游请求头 `RESPONSE_ID`。在 `doFinally` 回调中清理 MDC，防止 Reactor 线程池复用时的上下文污染。

- **SentinelGatewayFilter**（`Ordered.HIGHEST_PRECEDENCE + 1`）-- 紧随其后。由 Sentinel Gateway Adapter 提供，对匹配的路由执行流量控制。当请求被限流时，由 SentinelGatewayBlockExceptionHandler 返回限流响应。

- **SaReactorFilter** -- SA-Token 提供的响应式过滤器。拦截所有路径（`/**`），委托 CdpFilterAuthStrategy 执行认证逻辑。异常处理回调区分 UnauthorizedException 和 UncheckedException，返回对应的错误码和消息。

> 设计决策：将 Trace ID 生成放在最高优先级，确保后续所有过滤器（包括限流和认证）产生的日志都能携带链路 ID。Sentinel 过滤器在认证之前执行，使得限流在鉴权之前生效，避免被恶意请求消耗认证资源。

### 3.2 Strategy 模式：可替换的认证策略

CdpFilterAuthStrategy 实现了 SA-Token 的 SaFilterAuthStrategy 接口，将认证逻辑委托给 CdpAuthHandler。执行流程为：

1. 清除线程本地的 Token 上下文（`CdpTokenHolder.clear()`），避免 Reactor 线程复用导致的上下文串扰。
2. 获取当前请求 URI。
3. 调用 `cdpAuthHandler.checkAuthAndPermission(uri)` 执行统一的登录检查和 URI 级别的权限校验。

CdpAuthHandler 定义在 `leatop-cdp-common-auth` 模块中，其内部实现了白名单过滤、Token 有效性验证和 RBAC 权限检查。网关层通过依赖注入获取 CdpAuthHandler 实例，认证逻辑的具体实现对网关透明。

> 设计决策：认证策略以接口形式注入 SaReactorFilter，业务侧可通过自定义 SaFilterAuthStrategy Bean 完全替换默认认证逻辑，无需修改网关模块源码。GatewayAutoConfiguration 中 `getSaReactorFilter` 方法接收 `SaFilterAuthStrategy` 参数而非直接 new CdpFilterAuthStrategy，保证了策略的可替换性。

### 3.3 Observer 模式：规则变更的本地持久化

Sentinel 限流规则的管理采用"内存 + 本地文件"双写策略：

- **SentinelGatewayRuleConfig** 在 Bean 初始化时（`afterPropertiesSet`）从本地 JSON 文件加载网关流控规则和 API 分组定义。若 Sentinel Dashboard 已推送规则到内存，则跳过文件加载，避免覆盖。同时启动一个 1 秒周期的定时任务，轮询检测内存中的 GatewayFlowRule 是否发生变化，变化时写回本地文件。

- **LocalSaveApiDefinitionChangeObserver** 实现了 Sentinel 的 ApiDefinitionChangeObserver 接口，当 API 分组定义发生变更时，通过 FileWritableDataSource 将新规则持久化到本地 `apiDefinition.json` 文件。内部通过 `CollUtil.isEqualList` 对比避免重复写入。

> 设计决策：Sentinel 默认将规则存储在内存中，重启即丢失。CDP 选择本地文件持久化而非直接依赖 Nacos 持久化，原因是网关作为基础设施层，应减少对外部依赖的强绑定。本地文件方案在 Nacos 不可用时仍能使用上次缓存的规则，提高了网关的独立性。

### 3.4 自定义路由断言：AntPathRoutePredicateFactory

Spring Cloud Gateway 内置的 PathRoutePredicateFactory 基于 PathPattern 进行路径匹配，不支持 `/**/*.js` 等 Ant 风格的双通配符。AntPathRoutePredicateFactory 继承 AbstractRoutePredicateFactory，使用 Spring 的 AntPathMatcher 替代 PathPattern 进行匹配。

匹配成功后，工厂会将匹配到的路径模式写入 Exchange 属性（`GATEWAY_PREDICATE_MATCHED_PATH_ATTR`），与 Gateway 的内置属性体系保持一致，确保 StripPrefix 等内置过滤器能正确提取路径信息。

> 设计决策：通过新增一个自定义 Predicate 而非修改框架源码来扩展路径匹配能力。在路由配置中使用 `AntPath=/api/**/*.json` 即可启用 Ant 风格匹配，与内置 `Path` 断言并行工作，互不干扰。

## 四、关键类说明

| 类名 | 角色 | 核心职责 |
|------|------|---------|
| `GatewayAutoConfiguration` | 自动配置入口 | 注册 traceIdFilter、SentinelGatewayFilter、SentinelGatewayBlockExceptionHandler、SaReactorFilter |
| `CdpFilterAuthStrategy` | 认证策略 | 实现 SaFilterAuthStrategy，委托 CdpAuthHandler 完成登录和权限校验 |
| `SentinelGatewayRuleConfig` | 限流规则配置 | 从本地文件加载网关流控规则和 API 分组，定时同步内存规则到文件 |
| `CdpGatewayDefaultConfigListener` | 默认配置注入 | 监听 ApplicationEnvironmentPreparedEvent，注入 Sentinel 限流失败响应体和 Jasypt 加密器默认值 |
| `AntPathRoutePredicateFactory` | 路由断言工厂 | 基于 AntPathMatcher 实现 Ant 风格路径匹配，补充 Gateway 内置 PathPattern 的不足 |
| `LocalSaveApiDefinitionChangeObserver` | API 定义变更监听 | 实现 ApiDefinitionChangeObserver，将 API 分组定义变更持久化到本地文件 |
| `PrintFinishApplicationRunner` | 启动信息打印 | 应用启动完成后输出服务名、IP、端口和上下文路径 |

## 五、扩展机制

### 5.1 自定义认证策略

实现 SaFilterAuthStrategy 接口并注册为 Spring Bean，即可替换默认的 CdpFilterAuthStrategy。GatewayAutoConfiguration 通过方法参数注入策略，不存在硬编码绑定。例如，可实现基于 API Key 的认证策略，在网关层对第三方系统的调用进行独立鉴权。

### 5.2 自定义限流响应

CdpGatewayDefaultConfigListener 仅在 `spring.cloud.sentinel.scg.fallback.mode` 未配置时注入默认值。开发者在 YAML 中显式配置该属性即可覆盖默认的限流响应体，或切换为 `redirect` 模式重定向到降级页面。

### 5.3 自定义路由断言

参照 AntPathRoutePredicateFactory 的实现，继承 AbstractRoutePredicateFactory 并注册为 `@Component`，即可新增自定义路由断言。Gateway 通过类名前缀自动识别（去掉 `RoutePredicateFactory` 后缀后作为断言名称）。

### 5.4 Sentinel 规则持久化扩展

当前采用本地文件持久化。若需切换为 Nacos 持久化，可替换 SentinelGatewayRuleConfig 中的 ReadableDataSource 实现为 NacosDataSource，并在 pom 中引入 `sentinel-datasource-nacos` 依赖。由于 SentinelGatewayRuleConfig 使用 `@ConditionalOnProperty` 条件装配，可通过自定义配置类覆盖。

## 六、模块协作

- **leatop-cdp-common-auth** -- 提供 CdpAuthHandler、CdpTokenHolder 等认证基础设施。网关层的 SA-Token 使用 Reactor 版本（`sa-token-reactor-spring-boot3-starter`），排除了 Servlet 版本，避免 WebFlux 与 Web MVC 冲突。
- **leatop-cdp-common-jasypt** -- 提供 CustomEncryptor，网关通过 ComponentScan 扫描其所在包，使得网关配置文件中的 `ENC(...)` 密文可被自动解密。CdpGatewayDefaultConfigListener 同时注入 Jasypt 加密器 Bean 名称的默认值。
- **leatop-cdp-micro-limit** -- 网关依赖限流模块的 RuleUtils 和 CustomFileRefreshableDataSource 进行规则文件管理。网关额外引入了 `spring-cloud-alibaba-sentinel-gateway` 适配器，提供 Gateway 专属的限流能力（基于路由 ID 和 API 分组的限流维度）。
- **leatop-cdp-micro-discovery** -- 网关通过 `lb://service-name` 协议利用 Nacos 服务发现进行负载均衡转发。网关本身不使用 Feign，但依赖 LoadBalancer 客户端。
- **leatop-cdp-common-data** -- 提供 HeaderConstant（请求头常量）、CoreConstants（Trace ID 常量名）、ErrorCodeEnum（错误码枚举）等基础数据定义。

## 七、设计权衡与约束

1. **WebFlux 与 Servlet 互斥** -- 网关基于 WebFlux 响应式栈，不能引入任何 Servlet 依赖（包括 `spring-boot-starter-web`）。因此 SA-Token 必须使用 Reactor 版本，且 `leatop-cdp-common-auth` 中的 Servlet 版 SA-Token Starter 在 pom 中被排除。这意味着网关层无法复用基于 Servlet 的过滤器链（如 MDCRequestFilter、CheckCSRFFilter），需要用 GlobalFilter 重新实现相应功能。

2. **Sentinel 规则轮询同步** -- SentinelGatewayRuleConfig 使用 1 秒周期的定时任务轮询 GatewayFlowRule 变更并写入文件。这种轮询方式存在最多 1 秒的延迟，且在规则频繁变动时会产生较多的文件 I/O。选择轮询而非监听的原因是 GatewayRuleManager 不提供原生的变更回调接口（与 ApiDefinitionChangeObserver 不同）。

3. **CdpTokenContextForSpringReactor 已注释** -- 源码中 CdpTokenContextForSpringReactor 原本用于重写 SA-Token 的路径匹配逻辑（从 PathPattern 切换为 AntPathMatcher），但当前已被注释。这表明 SA-Token 的路径匹配已通过其他方式（如升级版本或配置）解决，AntPathRoutePredicateFactory 仅用于 Gateway 路由层面的匹配扩展。

4. **SentinelGatewayBlockExceptionHandler 的条件注册** -- 该 Handler 使用 `@ConditionalOnMissingBean` 注册，允许业务侧自定义限流异常处理。但由于 Gateway 的响应式特性，自定义 Handler 需要正确处理 ViewResolver 和 ServerCodecConfigurer 的注入，实现门槛较高。

5. **配置加密的双重保障** -- CdpGatewayDefaultConfigListener 和 GatewayAutoConfiguration 的 ComponentScan 共同确保 Jasypt 在网关环境中正常工作。Listener 在环境准备阶段注入 Bean 名称，ComponentScan 在应用上下文阶段扫描 CustomEncryptor 类。两者缺一不可。
