# 如何理解 CDP 过滤器链与请求处理机制

## 概述

CDP 框架在 `leatop-cdp-common-core` 中注册了一系列过滤器，按顺序处理每个 HTTP 请求。过滤器链负责 CSRF 防护、链路追踪、请求体可重复读和数据脱敏，业务代码无需手动配置。

## 过滤器执行顺序

```
请求进入 → CheckCSRFFilter(@Order=-1) → MDCRequestFilter(@Order=0)
  → RepeatedlyRequestFilter(@Order=1) → CdpSaInterceptor(认证拦截器)
  → Controller → GlobalResponseBodyHandler(数据脱敏) → 响应返回
```

## 各过滤器说明

### 1. CheckCSRFFilter（CSRF 防护，@Order=-1）

拦截 POST/PUT/DELETE 请求，校验请求来源是否合法。

```yaml
cdp:
  extension:
    csrf:
      allowed-origins:           # 允许的域名（支持通配符）
        - "*.example.com"
        - "localhost"
      check-headers:             # 校验的请求头
        - Referer
        - Origin
```

- 校验通过：继续后续过滤器
- 校验失败：返回 HTTP 403 + JSON 错误信息
- GET/HEAD/OPTIONS 请求不做校验

### 2. MDCRequestFilter（链路追踪，@Order=0）

为每个请求生成唯一的 Trace ID，写入 SLF4J MDC 上下文，便于日志追踪。

- 优先从请求头 `REQUEST_ID` 读取（微服务间传递）
- 无则自动生成 12 位 nanoId
- 写入响应头 `RESPONSE_ID`
- 请求结束后清除 MDC、DataPermissionHolder、TenantContentHolder

**日志配置示例（logback）：**

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{TRACE_ID}] %-5level %logger - %msg%n</pattern>
```

### 3. RepeatedlyRequestFilter（请求体可重复读，@Order=1）

将 `HttpServletRequest` 包装为 `RepeatedlyRequestWrapper`，使请求体（body）可以被多次读取。

- 非 multipart 请求：包装为可重复读的 Wrapper
- multipart 请求（文件上传）：不包装，直接传递
- 响应端：如配置了加密接口，使用 `CdpResponseWrapper` 包装

### 4. BaseRequestFilter（抽象基类）

所有自定义过滤器的基类，内置白名单机制：

- 自动跳过静态资源（CSS、JS、图片、HTML）
- 自动跳过 OPTIONS 预检请求
- 自动跳过 `/v2/**`、`/static/**` 路径

## 响应处理 — GlobalResponseBodyHandler

`@RestControllerAdvice` 实现，处理接口返回值的数据脱敏：

### @DataMasking 数据脱敏

```java
import com.leatop.cdp.core.annotation.DataMasking;
import com.leatop.cdp.core.annotation.DataMaskingField;

@RestController
public class UserController {

    @DataMasking  // 启用数据脱敏
    @GetMapping("/user/{id}")
    public Message<UserDTO> getUser(@PathVariable Long id) {
        return Message.success(userService.getById(id));
    }
}
```

```java
@Data
public class UserDTO {
    private String name;

    @DataMaskingField  // 该字段在响应时自动脱敏
    private String phone;

    @DataMaskingField
    private String idCard;
}
```

**脱敏规则：**
- 超级管理员（SUPER_ADMIN）和租户管理员（TENANT_ADMIN）不脱敏
- 微服务内部调用（携带 MICRO_TOKEN 头）不脱敏
- 其他角色按 `@DataMaskingField` 配置进行脱敏

## 注意事项

> 注意：过滤器链由框架自动注册，业务代码无需手动配置。如需添加自定义过滤器，建议 @Order 值大于 1。

> 注意：CSRF 防护默认开启，如前后端分离部署需在 `cdp.extension.csrf.allowed-origins` 中配置前端域名。

> 注意：Trace ID 通过 MDC 传递，在日志格式中使用 `%X{TRACE_ID}` 即可输出，便于全链路日志追踪。

> 注意：`@DataMasking` 注解需标注在 Controller 方法上，`@DataMaskingField` 标注在 DTO 字段上，两者配合使用。
