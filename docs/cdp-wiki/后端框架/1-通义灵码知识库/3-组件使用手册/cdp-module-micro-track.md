# 如何使用 CDP 链路追踪组件

## 概述

链路追踪组件（`leatop-cdp-micro-track`）基于 Apache Skywalking 10.0.1 实现，提供微服务调用链的全链路追踪和性能监控能力，帮助定位跨服务调用中的性能瓶颈和异常。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-track</artifactId>
</dependency>
```

## 部署方式

Skywalking 采用 Agent 方式接入，不侵入业务代码：

### 1. 下载 Skywalking Agent

从 Apache Skywalking 官网下载 Agent 包，解压到服务器。

### 2. JVM 参数方式接入

```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=my-service \
     -Dskywalking.collector.backend_service=172.17.1.83:11800 \
     -jar my-application.jar
```

### 3. 关键配置

| 参数 | 说明 |
|------|------|
| `skywalking.agent.service_name` | 服务名称，在 Skywalking UI 中展示 |
| `skywalking.collector.backend_service` | Skywalking OAP 服务器地址 |
| `skywalking.agent.sample_n_per_3_secs` | 每 3 秒采样数量（-1 表示全量采集） |

## 核心能力

- **调用链追踪**：自动记录 HTTP、RPC、数据库、Redis、MQ 等调用链
- **性能监控**：接口响应时间、吞吐量、SLA 统计
- **拓扑图**：自动生成微服务之间的调用关系拓扑图
- **告警**：基于规则的性能告警（响应时间超标、成功率下降等）

## 与 CDP 过滤器集成

CDP 的 `MDCRequestFilter` 会自动生成 Trace ID 并写入 MDC，日志中可通过 `%X{TRACE_ID}` 输出。在微服务间调用时，Trace ID 通过请求头自动传递，实现全链路日志关联。

## 注意事项

> 注意：Skywalking Agent 以 `-javaagent` 方式接入，无需修改业务代码。

> 注意：需先部署 Skywalking OAP Server 和 UI，Agent 连接 OAP 上报数据。

> 注意：生产环境建议配置采样率（`sample_n_per_3_secs`），避免全量采集带来的性能开销。

> 注意：Skywalking 自动追踪 Spring Cloud Gateway、OpenFeign、MyBatis 等组件，无需额外配置。
