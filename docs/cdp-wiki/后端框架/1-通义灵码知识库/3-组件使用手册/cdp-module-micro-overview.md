# CDP 微服务架构总览

## 概述

CDP 微服务组件基于 Spring Cloud Alibaba 微服务解决方案，整合 Nacos（服务注册与配置中心）、Sentinel（限流熔断）、Spring Cloud Gateway（服务网关）和 Skywalking（链路追踪）等组件。

## 技术栈

| 组件 | 说明 | 版本 |
|------|------|------|
| Spring Cloud | 微服务基础框架 | 2025.0.0 |
| Spring Cloud Alibaba | 阿里巴巴微服务方案 | 2025.0.0.0 |
| Nacos | 服务注册发现 + 配置中心 | 2.4.1 |
| Sentinel | 限流降级 | 1.8.7 |
| Spring Cloud Gateway | 服务网关 | — |
| Skywalking | 链路追踪 | 10.0.1 |

## 微服务模块清单

| 模块 | 说明 |
|------|------|
| `leatop-cdp-micro-core` | 微服务核心依赖（认证、Actuator） |
| `leatop-cdp-micro-config` | Nacos 配置中心集成 |
| `leatop-cdp-micro-discovery` | Nacos 服务注册发现 + OpenFeign |
| `leatop-cdp-micro-gateway` | Spring Cloud Gateway 网关 |
| `leatop-cdp-micro-limit` | Sentinel 限流熔断 |
| `leatop-cdp-micro-track` | Skywalking 链路追踪 |

## 单体与微服务切换

CDP 业务组件同时提供 `*-boot-starter` 和 `*-cloud-starter` 两个 Starter：

```xml
<!-- 单体部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
</dependency>

<!-- 微服务部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-cloud-starter</artifactId>
</dependency>
```

切换方式：替换 pom 依赖 + 调整 application.yaml 配置，业务代码（Controller/Service/Mapper）无需改动。

## 示例项目

微服务示例在 `leatop-cdp-example-micro` 目录下：

| 模块 | 说明 |
|------|------|
| `leatop-cdp-example-micro-gateway` | 网关示例 |
| `leatop-cdp-example-micro-provider` | 服务提供者示例 |
| `leatop-cdp-example-micro-consumer` | 服务消费者示例 |
| `leatop-cdp-example-micro-usercenter` | 系统管理微服务示例 |

## 注意事项

> 注意：微服务部署依赖 Nacos 服务端，需先部署 Nacos 并确保各微服务可连接。

> 注意：业务模块的 `@FeignClient` 接口在单体部署时走本地调用，微服务部署时走远程调用，无需代码改动。
