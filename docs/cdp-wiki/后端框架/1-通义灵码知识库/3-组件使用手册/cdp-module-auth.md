# 如何使用 CDP 认证授权机制

## 概述

认证授权模块（`leatop-cdp-common-auth`）基于 SA-Token 1.44.0 实现，采用 JWT 混合模式（`StpLogicJwtForMixin`）。提供统一的用户信息获取接口 `IUserHelper`、请求拦截认证、白名单路径配置和权限校验。

## 核心组件

### IUserHelper — 用户信息统一接口

```java
public interface IUserHelper {
    CurrentUserDto getCurrentUserInfo();           // 获取当前登录用户完整信息
    CurrentUserDto getCurrentUserInfoByToken(String token); // 根据 token 获取用户
    String getCurrentUserId();                     // 获取当前用户 ID
    String getTenantId();                          // 获取当前租户 ID
    List<String> getForbiddenPermissions();         // 获取无权限资源列表
    List<String> getRoleTypes();                    // 获取用户角色类型
    boolean isTenantAdmin();                       // 是否租户管理员
    boolean isLogin();                             // 是否已登录
    void logout();                                 // 登出
    void setCurrentUserInfo(CurrentUserDto user);  // 存储用户信息到会话
}
```

框架提供默认实现 `SysUserHelper`，基于 SA-Token Session 存储用户信息。

### 认证拦截器 CdpSaInterceptor

请求处理链中的认证环节：
1. 清除线程本地上下文
2. 解析请求中的 JWT Token
3. 调用 `CdpAuthHandler` 校验认证和权限
4. 设置租户上下文
5. 请求完成后清除上下文

### 白名单配置 CdpIgnoreProperties

```yaml
cdp:
  ignore:
    # 跳过 Token 校验的路径
    whites:
      - /login/**
      - /cas/**
      - /static/**
      - /swagger/**
      - /actuator/**
    # 跳过权限校验的路径（仍需登录）
    white-permissions:
      - /system/user/curUser
      - /system/resource/initMenu
```

## 使用示例

### 获取当前用户信息

```java
import com.leatop.cdp.core.api.IUserHelper;
import com.leatop.cdp.core.model.CurrentUserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    private IUserHelper userHelper;

    @GetMapping("/userInfo")
    public Message<CurrentUserDto> getUserInfo() {
        CurrentUserDto user = userHelper.getCurrentUserInfo();
        return Message.success(user);
    }

    @GetMapping("/myTenant")
    public Message<String> getMyTenant() {
        String tenantId = userHelper.getTenantId();
        String userId = userHelper.getCurrentUserId();
        return Message.success("tenant=" + tenantId + ", user=" + userId);
    }
}
```

### 登录与 Token 生成

登录由 `leatop-cdp-business-system-login` 模块提供，核心接口：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/login/web` | POST | 账号密码登录 |
| `/login/casCallBack` | GET | CAS SSO 回调 |
| `/login/accessToken` | GET | Token 换取登录 |
| `/login/logout` | GET | 登出 |

### 监听登录事件

```java
import com.leatop.cdp.system.event.LoginSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessEventListener {

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        // 登录成功后的自定义逻辑：记录日志、发送通知等
        String userId = event.getUserId();
    }
}
```

## SA-Token 配置

```yaml
sa-token:
  token-name: cdp-token           # Token 名称（请求头/Cookie 名）
  active-timeout: 36000            # Token 活跃超时时间（秒），10小时
  is-share: false                  # 同一账号是否共享 Token
  jwt-secret-key: "your-secret"    # JWT 签名密钥
```

## 权限校验流程

```
请求进入 → CdpSaInterceptor 拦截
  → 检查路径是否在白名单（whites）→ 是则跳过认证
  → 解析 Token，校验登录状态
  → 检查路径是否在权限白名单（white-permissions）→ 是则跳过权限检查
  → 加载用户无权限资源列表（forbiddenPermissions）
  → 匹配请求 URI 是否在无权限列表中 → 是则抛出 UnauthorizedException
  → 通过，继续执行业务逻辑
```

> 注意：CDP 采用"反向权限"模式 — 存储的是用户**无权限**的资源列表，不在列表中的资源默认有权限。

## 注意事项

> 注意：获取用户信息统一使用 `IUserHelper` 接口，不要直接调用 SA-Token 的 `StpUtil` API。

> 注意：Token 名称默认为 `cdp-token`，前端请求时需在请求头中携带 `cdp-token: {tokenValue}`。

> 注意：白名单路径使用 Ant 风格匹配（支持 `*`、`**`、`?`）。

> 注意：CAS 单点登录需额外引入 `leatop-cdp-business-syncdata-boot-starter` 并配置 `cdp.cas` 相关参数。

> 注意：自定义认证逻辑可实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean，框架会自动链式调用。
