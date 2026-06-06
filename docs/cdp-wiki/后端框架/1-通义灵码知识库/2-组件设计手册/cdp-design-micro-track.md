# CDP 链路追踪模块设计手册

> 对应使用手册：[cdp-module-micro-track.md](../3-组件使用手册/cdp-module-micro-track.md)

## 一、设计目标与背景

微服务架构下，一次用户请求可能跨越多个服务节点，问题定位的复杂度随服务数量指数增长。CDP 链路追踪模块提供分布式调用链追踪能力，帮助开发者跟踪请求在服务间的流转路径和性能指标。

设计目标如下：

1. **零侵入接入** -- 基于 Apache Skywalking 10.0.1 的 Java Agent 机制，通过 JVM 参数（`-javaagent`）方式接入，不修改任何业务代码，不引入编译期依赖。
2. **依赖声明即集成** -- 模块本身为纯 POM 模块（无自定义 Java 源码），仅作为依赖声明的占位符，表明服务已接入链路追踪能力。实际的追踪功能由运行时的 Skywalking Agent 提供。
3. **与 CDP 日志体系协同** -- CDP 的 MDCRequestFilter（单体模式）和 GatewayAutoConfiguration 中的 traceIdFilter（网关模式）在请求入口生成 Trace ID 并写入 MDC。Skywalking Agent 自动关联 MDC 中的 Trace ID，实现日志与调用链的统一关联。

## 二、整体架构

模块结构如下：

```
leatop-cdp-micro-track/
└── pom.xml       # 纯 POM 模块，无 Java 源码，无编译期依赖声明
```

该模块的 pom.xml 当前未声明任何 `<dependencies>`，其父模块 `leatop-cdp-micro` 提供了公共依赖管理。模块的存在意义有两方面：

1. **架构占位** -- 在模块矩阵中明确标识"链路追踪"这一横切关注点的归属位置，便于后续扩展（如引入 Micrometer Tracing 编译期依赖）。
2. **文档锚点** -- 为使用手册和设计手册提供明确的模块归属。

链路追踪的实际工作机制：

```
JVM 启动参数指定 -javaagent:skywalking-agent.jar
  -> Skywalking Agent 字节码增强
    -> 自动拦截 Spring Cloud Gateway、OpenFeign、MyBatis、Redis、MQ 等组件
    -> 生成 Trace/Span 数据
    -> 上报至 Skywalking OAP Server（gRPC 协议，默认端口 11800）
  -> Skywalking UI 展示拓扑图、调用链、性能指标
```

CDP 框架层面的 Trace ID 传递链路：

```
请求入口（MDCRequestFilter / traceIdFilter）
  -> 生成 Trace ID，写入 MDC 和请求头 REQUEST_ID
  -> FeignConfiguration.RequestInterceptor 将 REQUEST_ID 透传至下游服务
  -> 下游服务的 MDCRequestFilter 从请求头读取 REQUEST_ID，写入本地 MDC
  -> 日志输出格式中通过 %X{TRACE_ID} 打印链路 ID
```

> 设计决策：选择 Skywalking Agent 而非 Micrometer Tracing + Zipkin 等编译期方案，是因为 Agent 方式完全零侵入，不需要在 pom 中引入追踪相关依赖，也不需要在代码中添加任何注解或配置。对于已有大量业务代码的 CDP 框架来说，这种方式的接入成本最低。同时，CDP 通过 MDC 机制实现了自己的轻量级 Trace ID 传递，即使不部署 Skywalking，日志中仍然可以通过 REQUEST_ID 进行跨服务关联。

## 三、关键类说明

本模块不包含自定义 Java 类。与链路追踪相关的 CDP 框架类分布在其他模块中：

| 类名 | 所属模块 | 与链路追踪的关系 |
|------|---------|----------------|
| `MDCRequestFilter` | leatop-cdp-common-filter | 单体/微服务 Servlet 环境下生成 Trace ID 并写入 MDC |
| `GatewayAutoConfiguration.traceIdFilter` | leatop-cdp-micro-gateway | 网关 WebFlux 环境下生成 Trace ID 并注入下游请求头 |
| `FeignConfiguration.RequestInterceptor` | leatop-cdp-micro-discovery | Feign 调用时透传 REQUEST_ID 请求头 |
| `HeaderConstant` | leatop-cdp-common-data | 定义 REQUEST_ID、RESPONSE_ID 等请求头常量名 |
| `CoreConstants` | leatop-cdp-common-data | 定义 TRACE_ID MDC key 常量名 |
| `IdUtils` | leatop-cdp-common-util | 生成 12 位 NanoID 作为 Trace ID |

## 四、扩展机制

### 4.1 切换为编译期追踪方案

若需引入 Micrometer Tracing（Spring Boot 3.x 原生支持），可在本模块的 pom.xml 中添加 `micrometer-tracing-bridge-otel` 和对应的 Exporter 依赖（如 Zipkin 或 OTLP），并在自动配置中注册 Tracer Bean。模块的 POM 架构已预留了这一扩展空间。

### 4.2 自定义 Trace ID 生成策略

当前 Trace ID 由 `IdUtils.nanoId(12)` 生成 12 位随机字符串。若需与 Skywalking 的 Trace ID 格式对齐（如使用 Skywalking 的 TraceContext.traceId()），可自定义 MDCRequestFilter 或 traceIdFilter 的实现，从 Skywalking Agent 的上下文中获取 Trace ID。

### 4.3 采样率控制

Skywalking Agent 通过 `skywalking.agent.sample_n_per_3_secs` 参数控制采样率。-1 表示全量采集，生产环境建议根据流量规模设置合理的采样数（如每 3 秒采样 100 条），在可观测性和性能开销之间取得平衡。此配置通过 JVM 启动参数传入，与 CDP 框架代码无关。
