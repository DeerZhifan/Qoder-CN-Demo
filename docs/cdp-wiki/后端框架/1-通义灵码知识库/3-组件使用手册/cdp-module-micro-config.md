# 如何使用 CDP 配置中心组件

## 概述

配置中心组件（`leatop-cdp-micro-config`）基于 Nacos 实现配置的集中管理和动态刷新，支持多环境、多命名空间配置。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-micro-config</artifactId>
</dependency>
```

## 配置方式

### 方式一：spring.config.import（推荐）

```yaml
spring:
  cloud:
    nacos:
      config:
        group: DEFAULT_GROUP
        server-addr: 172.17.1.83:8848
  config:
    import:
      - optional:nacos:test.yml                             # 监听默认分组
      - optional:nacos:test01.yml?group=group_01            # 指定分组
      - optional:nacos:test02.yml?group=group_02&refreshEnabled=false  # 禁用动态刷新
      - nacos:test03.yml                                    # 不加 optional 表示必须存在
```

### 方式二：bootstrap.yml（兼容旧版）

需额外引入依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

```yaml
# bootstrap.yml
spring:
  cloud:
    nacos:
      config:
        name: test.yml
        group: DEFAULT_GROUP
        server-addr: 172.17.1.83:8848
        extension-configs:
          - dataId: test01.yml
            group: group_01
          - dataId: test02.yml
            group: group_02
            refresh: false
```

## 动态刷新

Nacos 配置修改后自动推送到应用，无需重启。使用 `@RefreshScope` 或 `@ConfigurationProperties` 的 Bean 会自动刷新。

```java
@RefreshScope
@RestController
public class ConfigController {

    @Value("${custom.config.key:default}")
    private String configValue;

    @GetMapping("/config")
    public String getConfig() {
        return configValue;  // 配置修改后自动更新
    }
}
```

## 注意事项

> 注意：使用 `spring.config.import` 方式时，不能同时使用 `bootstrap.yml`，两者互斥。

> 注意：`optional:` 前缀表示配置不存在时不报错，不加则配置缺失时启动失败。

> 注意：Nacos 2.4.0+ 版本首次启用鉴权后需手动初始化管理员密码。
