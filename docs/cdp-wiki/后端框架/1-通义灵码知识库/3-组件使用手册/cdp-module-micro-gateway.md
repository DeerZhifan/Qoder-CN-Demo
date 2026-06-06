# 如何使用 CDP 服务网关组件

## 概述

服务网关组件（`leatop-cdp-micro-gateway`）基于 Spring Cloud Gateway 构建，负责统一路由转发、请求过滤和负载均衡。网关作为微服务架构的流量入口，所有外部请求通过网关路由到后端微服务。

## 启用方式

```xml
<!-- 网关核心 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-gateway</artifactId>
</dependency>
<!-- 服务注册发现 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-discovery</artifactId>
</dependency>
<!-- 配置中心（可选） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-config</artifactId>
</dependency>
```

## 启动类

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

## 配置项

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_SERVERS:172.17.1.83:8848}
      username: ${NACOS_USER:nacos}
      password: ${NACOS_PWD:nacos}
      discovery:
        enabled: true
        namespace: ${NACOS_NS:dev}
    gateway:
      routes:
        - id: system-service
          uri: lb://system-service        # lb:// 表示通过 Nacos 负载均衡
          predicates:
            - Path=/system/**             # 匹配路径
          filters:
            - StripPrefix=0               # 转发时是否去除路径前缀
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
  config:
    import:
      - optional:nacos:${spring.application.name}?refreshEnabled=true
```

## 核心概念

| 概念 | 说明 |
|------|------|
| **Route（路由）** | 网关基本单元，由 ID、目标 URI、断言和过滤器组成 |
| **Predicate（断言）** | 匹配条件（Path、Header、Method 等） |
| **Filter（过滤器）** | 请求/响应处理（添加 Header、限流、鉴权等） |

## 注意事项

> 注意：网关项目不应引入 `spring-boot-starter-web`，Spring Cloud Gateway 基于 WebFlux（响应式），两者不兼容。

> 注意：`uri: lb://service-name` 中的 `service-name` 必须与 Nacos 注册的服务名一致。

> 注意：网关路由配置可放在 Nacos 配置中心，实现动态路由更新无需重启。

> 注意：生产环境建议在网关前面部署 Nginx，由 Nginx 负责 SSL 终结和静态资源服务。
