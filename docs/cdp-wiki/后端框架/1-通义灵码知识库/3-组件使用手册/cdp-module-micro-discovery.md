# 如何使用 CDP 服务注册与发现组件

## 概述

服务注册与发现组件（`leatop-cdp-micro-discovery`）基于 Nacos 实现服务注册和发现，集成 OpenFeign 实现声明式服务调用。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-discovery</artifactId>
</dependency>
```

## 配置项

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
    # 多网卡环境指定首选网段
    inetutils:
      preferred-networks:
        - 172.17.
```

## 使用步骤

### 1. 启动类添加注解

```java
@SpringBootApplication
@EnableDiscoveryClient  // 开启服务注册发现
public class ProviderApp {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApp.class, args);
    }
}
```

### 2. 定义 Feign 客户端

```java
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-provider")  // name = Nacos 注册的服务名
public interface EchoService {
    @GetMapping("/echo/{str}")
    String echo(@PathVariable("str") String str);
}
```

### 3. 注入调用

```java
@RestController
public class ConsumerController {

    @Autowired
    private EchoService echoService;

    @GetMapping("/call/{msg}")
    public String call(@PathVariable String msg) {
        return echoService.echo(msg);
    }
}
```

## @FeignClient 关键参数

| 参数 | 说明 |
|------|------|
| `name` / `value` | Nacos 注册的服务名称 |
| `url` | 直接指定服务地址（跳过服务发现） |
| `path` | 统一请求路径前缀 |
| `fallback` | 熔断降级处理类 |
| `fallbackFactory` | 熔断降级工厂类 |
| `configuration` | 自定义 Feign 配置（编码器、解码器等） |

## 注意事项

> 注意：`name` 同时指定 `url` 时，以 `url` 为准，`name` 仅作为客户端标识。

> 注意：`@FeignClient` 接口上不要添加 `@RequestMapping`，否则报错。

> 注意：参数中不要使用下划线 `_`，否则报 `Service id not legal hostname` 错误。

> 注意：多网卡环境需配置 `inetutils.preferred-networks`，确保注册正确的 IP 地址。
