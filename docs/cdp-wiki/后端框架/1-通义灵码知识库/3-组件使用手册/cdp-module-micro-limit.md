# 如何使用 CDP 限流熔断组件

## 概述

限流熔断组件（`leatop-cdp-micro-limit`）基于阿里巴巴 Sentinel 1.8.7 实现，提供流量控制、熔断降级和系统保护能力，防止微服务在高并发场景下被压垮。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-limit</artifactId>
</dependency>
```

## 配置项

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 172.17.1.83:8080   # Sentinel Dashboard 地址
        port: 8719                     # 与 Dashboard 通信端口
      eager: true                      # 立即初始化（默认懒加载）
```

## 核心能力

### 流量控制

限制接口的 QPS（每秒请求数），超出阈值的请求被拒绝或排队：
- **直接拒绝**：超过阈值直接返回错误
- **Warm Up**：预热模式，逐步增加通过的请求
- **排队等待**：匀速通过，削峰填谷

### 熔断降级

当接口异常比例或响应时间超过阈值时自动熔断，一段时间后尝试恢复：
- **慢调用比例**：响应时间超过阈值的比例达标则熔断
- **异常比例**：异常请求占比达标则熔断
- **异常数**：异常请求数达标则熔断

### 系统保护

从系统整体维度保护，当系统负载、CPU 使用率、线程数等超过阈值时触发保护。

## Sentinel Dashboard

Sentinel 提供可视化控制台，支持实时监控和动态规则配置：

```bash
# 启动 Sentinel Dashboard
java -jar sentinel-dashboard-1.8.7.jar --server.port=8080
```

访问 `http://172.17.1.83:8080`，默认账号密码 `sentinel/sentinel`。

## 注意事项

> 注意：Sentinel Dashboard 需独立部署，应用通过 `spring.cloud.sentinel.transport.dashboard` 连接。

> 注意：Sentinel 规则默认存储在内存中，重启丢失。生产环境建议持久化到 Nacos。

> 注意：`eager: true` 使 Sentinel 在应用启动时立即初始化，否则需要首次请求后才在 Dashboard 中可见。
