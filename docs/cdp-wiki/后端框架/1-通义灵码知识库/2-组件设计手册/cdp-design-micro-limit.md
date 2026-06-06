# CDP 限流熔断模块设计手册

> 对应使用手册：[cdp-module-micro-limit.md](../3-组件使用手册/cdp-module-micro-limit.md)

## 一、设计目标与背景

微服务架构中，某个服务的过载或故障可能引发级联雪崩。CDP 限流熔断模块基于 Alibaba Sentinel 1.8.7 构建，为业务微服务提供流量控制、熔断降级和系统保护能力。

设计目标如下：

1. **规则本地持久化** -- Sentinel 默认将规则存储在内存中，重启即丢失。CDP 实现了"Dashboard 推送 -> 内存更新 -> 本地文件回写"的双写机制，确保应用重启后可从本地文件恢复上次的规则配置。
2. **默认可用** -- 通过 ApplicationListener 和 EnvironmentPostProcessor 在应用启动早期注入合理的默认配置（如 `feign.sentinel.enabled=false`），无需业务侧额外配置即可使用。
3. **统一限流响应** -- 提供 CdpBlockExceptionHandler，在 Servlet 环境下将限流异常统一转换为 HTTP 429 状态码和标准的 JSON 错误响应。
4. **容错的文件数据源** -- 原生 FileRefreshableDataSource 在文件为空时会抛出异常。CDP 通过 CustomFileRefreshableDataSource 修复了这一问题，使空文件场景下优雅降级为空规则集。

## 二、整体架构

模块包结构如下：

```
com.leatop.cdp.micro.limit
├── CdpMicroLimitAutoConfiguration          # 自动配置入口
├── callback/
│   └── CdpBlockExceptionHandler            # Servlet 环境限流异常处理器
├── config/
│   ├── SentinelRuleConfig                  # 流控/降级规则初始化与本地文件持久化
│   ├── SentinelPropApplicationListener     # 应用环境准备阶段初始化规则路径和默认配置
│   └── FeignEnvironmentPostProcessor       # 设置 feign.sentinel.enabled 默认值
├── datasource/
│   └── CustomFileRefreshableDataSource     # 修复空文件异常的自定义文件数据源
└── util/
    └── RuleUtils                           # 规则文件路径管理和 JSON 序列化工具
```

自动配置的启动时序：

```
1. SentinelPropApplicationListener（spring.factories 注册）
   -> 监听 ApplicationEnvironmentPreparedEvent
   -> 初始化 RuleUtils 的 rulePath 和 appName
   -> 注入 feign.sentinel.enabled=false 默认值

2. Spring Boot 自动配置阶段
   -> CdpMicroLimitAutoConfiguration 触发 ComponentScan
   -> SentinelRuleConfig（@DependsOn SentinelAutoConfiguration）初始化
     -> 从本地文件加载 FlowRule 和 DegradeRule
     -> 注册 WritableDataSource 到 WritableDataSourceRegistry
   -> CdpBlockExceptionHandler 注册为限流异常处理器
```

## 三、关键类说明

| 类名 | 角色 | 核心职责 |
|------|------|---------|
| `CdpMicroLimitAutoConfiguration` | 自动配置 | 通过 `@ComponentScan` 扫描并加载模块内所有组件 |
| `SentinelRuleConfig` | 规则初始化 | 从本地 JSON 文件加载流控规则（FlowRule）和降级规则（DegradeRule），注册可读/可写数据源 |
| `SentinelPropApplicationListener` | 环境初始化 | 在 ApplicationEnvironmentPreparedEvent 阶段设置规则文件存储路径和 Feign Sentinel 默认值 |
| `FeignEnvironmentPostProcessor` | 环境后处理 | 作为 EnvironmentPostProcessor 设置 `feign.sentinel.enabled` 默认值为 false |
| `CdpBlockExceptionHandler` | 限流异常处理 | Servlet 环境下返回 HTTP 429 和 JSON 格式错误信息 |
| `CustomFileRefreshableDataSource` | 文件数据源 | 继承 AutoRefreshDataSource，修复原生类在空文件时的异常，3 秒周期轮询文件变更 |
| `RuleUtils` | 工具类 | 管理规则文件路径（按应用名分目录）、JSON 序列化/反序列化 |

> 设计决策：SentinelRuleConfig 使用 `@DependsOn("com.alibaba.cloud.sentinel.custom.SentinelAutoConfiguration")` 确保在 Sentinel 核心初始化完成后再加载本地规则文件。这是因为 Sentinel 的 FlowRuleManager 在 SentinelAutoConfiguration 中被初始化，过早注册数据源会导致空指针异常。

> 设计决策：`feign.sentinel.enabled` 默认设为 false，即默认不开启 Sentinel 对 Feign 的熔断保护。这是因为启用后 Feign 调用需要配置 fallback/fallbackFactory，否则异常处理行为会改变。业务侧需要显式配置为 true 并提供降级逻辑后才启用。

## 四、扩展机制

### 4.1 自定义规则存储路径

通过 `cdp.limit.rulePath` 属性可修改规则文件的存储根路径，默认为 `${user.home}/rules`。规则文件按应用名分目录存储（`{rulePath}/{appName}/flowRule.json`），多个应用实例可共享同一台服务器而互不干扰。

### 4.2 自定义限流异常响应

CdpBlockExceptionHandler 使用 `@ConditionalOnProperty(name = "spring.cloud.sentinel.filter.enabled", matchIfMissing = true)` 条件注册。业务侧可通过实现 BlockExceptionHandler 接口并注册为 Bean 替换默认行为，例如返回自定义错误码或根据不同的 BlockException 子类返回差异化响应。

### 4.3 网关场景扩展

`leatop-cdp-micro-gateway` 模块依赖本模块，并在此基础上通过 SentinelGatewayRuleConfig 增加了网关专属的 GatewayFlowRule 和 ApiDefinition 规则管理。本模块的 RuleUtils 和 CustomFileRefreshableDataSource 被网关模块直接复用，体现了基础能力的下沉设计。

### 4.4 Nacos 规则持久化

模块依赖中已包含 `sentinel-datasource-nacos`。若需将规则从本地文件切换为 Nacos 持久化，可自定义配置类替换 SentinelRuleConfig 中的 ReadableDataSource 实现为 NacosDataSource，实现 Dashboard -> Nacos -> 应用的规则推送链路。
