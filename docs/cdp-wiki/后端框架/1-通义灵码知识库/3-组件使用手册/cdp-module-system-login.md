# 如何使用 CDP 登录认证功能

## 概述

登录认证模块（`leatop-cdp-business-system-login`）提供多种登录方式：账号密码登录、CAS 单点登录、Token 换登录、验证码登录。基于 SA-Token JWT 生成和管理 Token。

## 核心接口

**Controller 路径前缀：** `/login`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/login/web` | POST | 账号密码登录 |
| `/login/accessToken` | GET | Token 换取登录 |
| `/login/casCallBack` | GET | CAS SSO 回调 |
| `/login/casLogin` | POST | CAS 登录 |
| `/login/logout` | POST | 登出 |
| `/login/changePwd` | POST | 首次登录强制修改密码 |
| `/login/captcha/getCode` | POST | 获取图形验证码 |
| `/login/getLoginCode` | POST | 获取短信/邮件验证码（type: 1=短信, 2=邮件） |

## 登录方式

### 方式一：账号密码登录

```
POST /login/web
Body: { "account": "admin", "password": "加密后的密码", "tenantCode": "租户编码" }
Response: { "code": 200, "data": { "token": "xxx", "user": {...} } }
```

### 方式二：CAS 单点登录

```yaml
cdp:
  cas:
    enabled: true
    servers:
      - id: 1
        server-url: http://cas-server:8080/cas
    client-prefix-url: http://localhost:28080
    call-back-page-prefix-url: http://localhost:9527/web
```

CAS 登录流程：浏览器重定向到 CAS → 用户认证 → 回调 `/login/casCallBack` → 生成 Token → 重定向前端

### 方式三：Token 换取登录

```
GET /login/accessToken?token=第三方Token
```

适用于第三方系统集成场景。

## 登录事件监听

```java
import com.leatop.cdp.system.event.LoginSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoginEventListener {

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        // 登录成功回调：记录日志、初始化用户数据等
    }
}
```

## 注意事项

> 注意：登录成功返回 `LoginResultDto`，包含 `token` 和用户信息，前端需将 token 存储并在后续请求中通过 `cdp-token` 请求头携带。

> 注意：首次登录或密码过期时，返回特定状态码，前端需引导用户调用 `/login/changePwd` 修改密码。

> 注意：CAS 集成需额外引入 `leatop-cdp-business-syncdata-boot-starter` 依赖，用于同步 CAS 用户到本地。

> 注意：登录接口路径 `/login/**` 在认证白名单中，不需要 Token 即可访问。
