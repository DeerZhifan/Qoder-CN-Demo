# 如何使用 CDP 异常处理机制

## 概述

CDP 框架在 `leatop-cdp-common-core` 和 `leatop-cdp-common-data` 中提供了统一的异常处理机制，包括业务异常类、标准错误码、统一响应包装和全局异常处理器。业务代码只需抛出框架异常，全局处理器自动将其转换为标准 JSON 响应。

## 异常类型体系

| 异常类 | 用途 | HTTP 状态码 | 所在包 |
|--------|------|------------|--------|
| `BusException` | 业务校验失败（参数不合法、数据不存在等） | 400 | `leatop-cdp-common-data` |
| `UncheckedException` | 系统运行时错误（外部服务调用失败等） | 400/500 | `leatop-cdp-common-data` |
| `ServiceUncheckedException` | 服务级异常，支持 i18n 参数 | 500 | `leatop-cdp-common-data` |
| `UnauthorizedException` | 认证/授权失败 | 401 | `leatop-cdp-common-data` |
| `ValidateException` | 参数校验异常 | 400 | `leatop-cdp-common-data` |

## 标准错误码

`ErrorCodeEnum` 定义了框架标准错误码：

| 枚举值 | code | msg |
|--------|------|-----|
| `SUCCESS` | 200 | 成功 |
| `FAIL` | 400 | 处理失败 |
| `ERROR` | 500 | 服务异常 |
| `UNAUTHORIZED` | 401 | 用户未登录 |
| `EXPIRED` | 406 | 认证信息已失效 |
| `NO_PERMISSIONS` | 407 | 无操作权限 |
| `ILLEGAL_PARAMS` | 60001 | 不合法参数 |

## 统一响应包装

所有接口统一返回 `Message<T>` 对象：

```java
import com.leatop.cdp.data.message.Message;

// 成功
Message.success();                    // { "code": 200, "msg": "成功", "data": null }
Message.success(data);                // { "code": 200, "msg": "成功", "data": {...} }

// 失败
Message.fail("处理失败");              // { "code": 400, "msg": "处理失败", "data": null }
Message.fail(500, "服务异常");         // { "code": 500, "msg": "服务异常", "data": null }
Message.fail(ErrorCodeEnum.UNAUTHORIZED); // { "code": 401, "msg": "用户未登录" }
```

## 使用示例

### 业务校验 — 使用 BusException

```java
import com.leatop.cdp.data.exception.BusException;

@Service
public class UserService {

    public UserDTO getUser(Long id) {
        if (id == null) {
            throw new BusException("用户ID不能为空");
        }
        UserPO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusException("用户不存在");
        }
        return convert(user);
    }
}
// 全局处理器自动返回：{ "code": 500, "msg": "用户不存在" }
```

### 自定义错误码

```java
throw new BusException("不支持的操作类型", 40001);
// 返回：{ "code": 40001, "msg": "不支持的操作类型" }
```

### 系统级错误 — 使用 UncheckedException

```java
import com.leatop.cdp.data.exception.UncheckedException;

@Service
public class ExternalService {

    public String callRemote() {
        try {
            return httpClient.get("http://remote-service/api");
        } catch (Exception e) {
            throw new UncheckedException("调用外部服务失败", e);
        }
    }
}
```

### Controller 中的标准写法

```java
import com.leatop.cdp.data.message.Message;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Message<UserDTO> getUser(@PathVariable Long id) {
        // 直接调用 Service，异常由全局处理器统一处理
        return Message.success(userService.getUser(id));
    }
}
```

## 全局异常处理器映射规则

`GlobalExceptionHandler`（`leatop-cdp-common-core`）自动处理以下异常：

| 异常类型 | HTTP 状态码 | 响应 code |
|---------|------------|-----------|
| `BusException` | 400 | `e.getErrorCode()` |
| `UnauthorizedException` | 401 | `e.getCode()` |
| `UncheckedException` | 400 | `e.getCode()` |
| `ServiceUncheckedException` | 500 | 500 |
| `BindException`（参数校验） | 400 | 60001 |
| `MissingServletRequestParameterException` | 400 | 500 |
| `HttpRequestMethodNotSupportedException` | 400 | 60001 |
| `NoResourceFoundException` | 404 | 404 |
| 其他未知异常 | 500 | 500 |

> 注意：处理器会递归查找异常链（最多 10 层），如果根因是 `BusException` 会按业务异常处理。

## 注意事项

> 注意：不要在 Controller/Service 中 try-catch 后吞掉异常，应让异常向上抛出由全局处理器统一处理。

> 注意：不要直接抛 `RuntimeException` 或 `IllegalArgumentException`，使用框架提供的异常类型。

> 注意：业务校验失败用 `BusException`，系统级错误用 `UncheckedException`，认证失败用 `UnauthorizedException`。

> 注意：全局异常处理器已在 `leatop-cdp-common-core` 中注册，业务模块无需额外配置。
