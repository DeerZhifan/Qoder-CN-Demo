---
trigger: when_referenced
knowledge_source:
  - cdp-design-micro-config
  - cdp-design-micro-discovery
  - cdp-design-micro-gateway
  - cdp-design-micro-limit
  - cdp-design-micro-track
  - cdp-module-micro-overview
  - cdp-module-micro-config
  - cdp-module-micro-discovery
  - cdp-module-micro-gateway
  - cdp-module-micro-limit
  - cdp-module-micro-track
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-micro-*` 系列依赖
- 使用 `@EnableDiscoveryClient`、`@FeignClient` 注解
- 配置 `spring.cloud.nacos.*`、`spring.cloud.gateway.*`、`spring.cloud.sentinel.*`
- 使用 Skywalking Agent（`-javaagent` 方式）
- 业务模块从 `*-boot-starter` 切换为 `*-cloud-starter`

---

## 前置依赖

微服务模块按需引入，各模块独立：

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
<!-- 服务网关 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-gateway</artifactId>
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

外部依赖：Nacos Server（2.4.1+）、Sentinel Dashboard（1.8.7）、Skywalking OAP Server（10.0.1）。

---

## 配置要点

### Nacos 配置中心（推荐 spring.config.import 方式）

```yaml
spring:
  cloud:
    nacos:
      config:
        group: DEFAULT_GROUP
        server-addr: ${NACOS_SERVERS:172.17.1.83:8848}
  config:
    import:
      - optional:nacos:${spring.application.name}.yml        # 监听默认分组
      - optional:nacos:shared.yml?group=SHARED_GROUP          # 共享配置
```

### Nacos 服务注册发现

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_SERVERS:172.17.1.83:8848}
      username: ${NACOS_USER:nacos}
      password: ${NACOS_PWD:!QAZ2wsx}
      discovery:
        enabled: true
        namespace: ${NACOS_NS:dev}
    inetutils:
      preferred-networks:
        - 172.17.
```

### Gateway 路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: system-service
          uri: lb://system-service       # lb:// 通过 Nacos 负载均衡
          predicates:
            - Path=/system/**
          filters:
            - StripPrefix=0
```

### Sentinel 限流

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 172.17.1.83:8080
        port: 8719
      eager: true                        # 立即初始化
```

### Skywalking 链路追踪

通过 JVM 参数接入，不侵入代码：

```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=my-service \
     -Dskywalking.collector.backend_service=172.17.1.83:11800 \
     -jar my-application.jar
```

---

## 代码模式

### 推荐写法

**Feign 声明式服务调用：**

```java
@FeignClient(name = "service-provider")
public interface EchoService {
    @GetMapping("/echo/{str}")
    String echo(@PathVariable("str") String str);
}
```

**单体/微服务切换：** 替换 `*-boot-starter` 为 `*-cloud-starter`，业务代码不变：

```xml
<!-- 微服务部署时 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-cloud-starter</artifactId>
</dependency>
```

**动态配置刷新：**

```java
@RefreshScope
@RestController
public class ConfigController {
    @Value("${custom.config.key:default}")
    private String configValue;
}
```

### 禁止事项

- **禁止网关项目引入 `spring-boot-starter-web`** -- Gateway 基于 WebFlux，与 Web MVC 不兼容
- **禁止在 `@FeignClient` 接口上添加 `@RequestMapping`** -- 会导致路径冲突报错
- **禁止 Feign 参数名使用下划线** -- 会触发 `Service id not legal hostname` 错误
- **禁止同时使用 `spring.config.import` 和 `bootstrap.yml`** -- 两者互斥
- **禁止 Sentinel 规则仅存内存** -- 生产环境必须持久化到 Nacos，避免重启丢失
- **禁止 Skywalking 生产环境全量采集** -- 必须配置 `sample_n_per_3_secs` 采样率
- **禁止硬编码 Nacos 地址** -- 必须使用环境变量 `${NACOS_SERVERS:默认值}`
