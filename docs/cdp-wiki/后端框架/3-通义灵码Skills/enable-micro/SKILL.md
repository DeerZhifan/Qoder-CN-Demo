# 启用 CDP 微服务治理

## 描述

将已有 CDP 单体应用迁移为微服务架构，或搭建全新的微服务项目。启用 Nacos 配置中心 + 服务注册发现、Spring Cloud Gateway 网关、Sentinel 限流熔断、Skywalking 链路追踪全套微服务治理能力。

## 输入

请向用户确认以下信息：

1. **项目类型**（新建微服务项目 / 单体迁移微服务）
2. **需要启用的组件**（config / discovery / gateway / limit / track，默认全部）
3. **Nacos 地址**（默认 `172.17.1.83:8848`）
4. **Sentinel Dashboard 地址**（默认 `172.17.1.83:8080`）
5. **Skywalking OAP 地址**（默认 `172.17.1.83:11800`）

---

## 步骤 1：添加 Maven 依赖

> 微服务模块按需引入。版本号由 BOM 管理，不需要手动指定。

**业务服务（Provider / Consumer）：**

```xml
<!-- 配置中心 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-config</artifactId>
</dependency>
<!-- 服务注册发现 + OpenFeign -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-discovery</artifactId>
</dependency>
<!-- 限流熔断 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-limit</artifactId>
</dependency>
<!-- 链路追踪 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-track</artifactId>
</dependency>
```

**网关服务（独立项目）：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-gateway</artifactId>
</dependency>
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-config</artifactId>
</dependency>
```

**单体迁移微服务时**，将业务模块依赖从 `*-boot-starter` 替换为 `*-cloud-starter`：

```xml
<!-- 替换前 -->
<artifactId>leatop-cdp-business-system-boot-starter</artifactId>
<!-- 替换后 -->
<artifactId>leatop-cdp-business-system-cloud-starter</artifactId>
```

## 步骤 2：配置 Nacos 配置中心

> 推荐使用 `spring.config.import` 方式（Spring Boot 3.x），不要与 `bootstrap.yml` 混用。

在 `application.yaml` 中添加：

```yaml
spring:
  cloud:
    nacos:
      config:
        group: DEFAULT_GROUP
        server-addr: ${NACOS_SERVERS:{Nacos地址}}
  config:
    import:
      - optional:nacos:${spring.application.name}.yml
      - optional:nacos:shared.yml?group=SHARED_GROUP
```

## 步骤 3：配置服务注册发现

在 `application.yaml` 中添加：

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_SERVERS:{Nacos地址}}
      username: ${NACOS_USER:nacos}
      password: ${NACOS_PWD:!QAZ2wsx}
      discovery:
        enabled: true
        namespace: ${NACOS_NS:dev}
    inetutils:
      preferred-networks:
        - 172.17.
```

启动类添加注解：

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ProviderApp {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApp.class, args);
    }
}
```

## 步骤 4：配置 Gateway 路由（仅网关服务）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: system-service
          uri: lb://system-service
          predicates:
            - Path=/system/**
          filters:
            - StripPrefix=0
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
```

## 步骤 5：配置 Sentinel 限流

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:{Sentinel地址}}
        port: 8719
      eager: true
```

## 步骤 6：配置 Skywalking 链路追踪

在启动脚本中添加 JVM 参数：

```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=${spring.application.name} \
     -Dskywalking.collector.backend_service={Skywalking地址} \
     -jar my-application.jar
```

## 步骤 7：定义 Feign 客户端（服务间调用）

```java
@FeignClient(name = "service-provider")
public interface EchoService {
    @GetMapping("/echo/{str}")
    String echo(@PathVariable("str") String str);
}
```

## 步骤 8：验证

1. 启动 Nacos Server，确认服务注册成功（Nacos 管理界面可见服务实例）
2. 启动网关，确认路由转发正常
3. 访问 Sentinel Dashboard，确认应用已连接
4. 检查 Skywalking UI，确认调用链数据上报

---

## 完成后提醒

1. 网关项目**不能**引入 `spring-boot-starter-web`（WebFlux 与 Web MVC 不兼容）
2. `@FeignClient` 接口上**不要**添加 `@RequestMapping`，参数名**不要**使用下划线
3. Sentinel 规则生产环境**必须**持久化到 Nacos，避免重启丢失
4. Skywalking 生产环境**必须**配置采样率 `sample_n_per_3_secs`，避免全量采集的性能开销
5. 多网卡环境**必须**配置 `inetutils.preferred-networks`，确保注册正确的 IP
6. 参考示例项目 `leatop-cdp-example-micro` 了解完整的微服务部署架构
