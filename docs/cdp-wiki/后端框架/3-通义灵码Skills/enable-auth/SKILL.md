# 启用 CDP 认证授权

## 描述

在已有 CDP 项目中启用认证授权模块（`leatop-cdp-common-auth`），基于 SA-Token 1.44.0 实现 JWT 混合模式认证。提供统一的用户信息获取接口 `IUserHelper`、请求拦截认证、白名单路径配置和反向权限校验。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-example-demo1`）
2. **JWT 签名密钥**（`sa-token.jwt-secret-key`，必须配置）
3. **Token 活跃超时时间**（秒，默认 `36000` 即 10 小时）
4. **需要跳过认证的白名单路径**（如 `/login/**`、`/public/**`）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-common-auth` 通常通过 `leatop-cdp-common-starter` 自动引入。如果项目未引入 starter，需手动添加。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中确认存在：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-common-starter</artifactId>
</dependency>
```

如果仅需认证模块而不引入完整 starter，可单独添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-common-auth</artifactId>
</dependency>
```

## 步骤 2：SA-Token 配置

> SA-Token 参数通过 `CdpTokenConfigure` 以代码方式设置，框架默认值优先级高于 YAML。以下为可覆盖的配置项。

在 `application.yaml` 中添加：

```yaml
sa-token:
  token-name: cdp-token           # Token 名称（请求头/Cookie 名）
  active-timeout: {Token活跃超时时间}  # Token 活跃超时时间（秒），默认 36000
  is-share: false                  # 同一账号不共享 Token
  jwt-secret-key: "{JWT签名密钥}"    # JWT 签名密钥（必须修改为安全密钥）
```

## 步骤 3：白名单配置

> 白名单路径使用 Ant 风格匹配（支持 `*`、`**`、`?`）。`whites` 中的路径跳过 Token 校验，`white-permissions` 中的路径仍需登录但跳过权限校验。

在 `application.yaml` 中添加：

```yaml
cdp:
  ignore:
    whites:
      - /login/**
      - /cas/**
      - /static/**
      - /swagger/**
      - /actuator/**
      # 按需追加自定义白名单路径
    white-permissions:
      - /system/user/curUser
      - /system/resource/initMenu
      # 按需追加跳过权限校验的路径
```

## 步骤 4：编写示例代码

> 获取用户信息统一使用 `IUserHelper` 接口，不要直接调用 SA-Token 的 `StpUtil` API。

**获取当前用户信息：**

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

**监听登录事件（可选）：**

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

**自定义认证方式（可选）：**

```java
import com.leatop.cdp.auth.api.AuthHandlerExecutor;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthHandlerExecutor implements AuthHandlerExecutor {
    @Override
    public boolean checkAuthAndPermission(String uri) {
        // 检测请求中是否携带自定义认证凭证
        // 如果匹配，完成认证并返回 true（短路后续处理器）
        // 如果不匹配，返回 false，交给下一个处理器
        return false;
    }
}
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 访问白名单路径（如 `/login/web`）不需要 Token 即可访问
3. 访问非白名单路径，不携带 Token 时返回 401 未授权
4. 携带有效 Token 访问接口，`IUserHelper.getCurrentUserInfo()` 返回正确的用户信息
5. 前端请求头中携带 `cdp-token: {tokenValue}`

---

## 完成后提醒

1. 前端请求时需在请求头中携带 `cdp-token: {tokenValue}`，Token 名称默认为 `cdp-token`
2. 获取用户信息必须通过 `IUserHelper` 接口，禁止直接调用 `StpUtil` 静态方法
3. CDP 采用"反向权限"模式 -- 存储的是用户**无权限**的资源列表，不在列表中的资源默认有权限
4. CAS 单点登录需额外引入 `leatop-cdp-business-syncdata-boot-starter` 并配置 `cdp.cas` 相关参数
5. 自定义认证逻辑可实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean，框架自动链式调用
6. `jwt-secret-key` 为敏感信息，生产环境建议通过环境变量或 Jasypt 加密传递
