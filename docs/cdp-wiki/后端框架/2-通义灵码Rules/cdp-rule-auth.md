---
trigger: when_referenced
knowledge_source:
  - cdp-design-auth
  - cdp-module-auth
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-common-auth` 依赖
- 使用 `IUserHelper` 接口获取用户信息
- 使用 SA-Token 相关注解（`@SaCheckPermission`、`@SaCheckRole`、`@SaCheckLogin`）
- 配置白名单路径（`cdp.ignore.whites`、`cdp.ignore.white-permissions`）
- 实现 `AuthHandlerExecutor` 自定义认证策略
- 涉及登录、登出、Token 管理逻辑

---

## 前置依赖

1. Maven 依赖（通过 `leatop-cdp-common-starter` 自动引入，通常无需单独添加）：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-common-auth</artifactId>
</dependency>
```

2. 认证模块依赖 `leatop-cdp-common-core`（提供 `IUserHelper`、`CdpTokenHolder`）和 `leatop-cdp-common-data`（提供 `BaseUserInfo`、`CurrentUserDto`）。

3. 需要 Redis 连接配置，SA-Token Session 存储依赖 Redis。

---

## 配置要点

### SA-Token 核心配置

```yaml
sa-token:
  token-name: cdp-token           # Token 名称（请求头名），前端需携带 cdp-token: {tokenValue}
  active-timeout: 36000            # Token 活跃超时时间（秒），默认 10 小时
  is-share: false                  # 同一账号不共享 Token
  jwt-secret-key: "your-secret"    # JWT 签名密钥（生产环境必须修改）
```

### 白名单配置

```yaml
cdp:
  ignore:
    # 跳过 Token 校验的路径（Ant 风格，支持 *、**、?）
    whites:
      - /login/**
      - /cas/**
      - /static/**
      - /swagger/**
      - /actuator/**
    # 跳过权限校验但仍需登录的路径
    white-permissions:
      - /system/user/curUser
      - /system/resource/initMenu
```

### Token 续期策略

框架在 `CdpAuthHandler.refreshTokenTime()` 中实现自动续期：当 Token 活跃时间接近超时时自动刷新。续期由拦截器透明完成，业务代码无需干预。

---

## 代码模式

### 推荐写法

**获取用户信息 -- 统一使用 IUserHelper 接口：**

```java
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

**监听登录事件：**

```java
@Component
public class LoginSuccessEventListener {

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        String userId = event.getUserId();
        // 登录成功后的自定义逻辑：记录日志、发送通知等
    }
}
```

**自定义认证方式 -- 实现 AuthHandlerExecutor：**

```java
@Component
public class OAuth2AuthHandlerExecutor implements AuthHandlerExecutor {
    @Override
    public boolean checkAuthAndPermission(String uri) {
        // 检测请求中是否携带 OAuth2 Bearer Token
        // 如果是 OAuth2 请求，完成认证并返回 true
        // 如果不是，返回 false，交给下一个处理器
        return false;
    }
}
```

**替换权限加载策略 -- 实现 PermissionManage：**

通过 Spring Bean 注册 `PermissionManage` 实现类，SysUserHelper 会自动注入并从中实时加载权限列表，替代 SA-Token Session 中的缓存数据。

### 禁止事项

- **禁止直接调用 `StpUtil` 静态方法获取用户信息** -- 必须通过 `IUserHelper` 接口，否则业务代码与 SA-Token 强耦合，无法切换认证方案或进行单元测试
- **禁止手动管理 `CdpTokenHolder` / `TenantContentHolder` 的生命周期** -- 由 `CdpSaInterceptor` 在请求前后自动设置和清理，手动操作可能导致内存泄漏或数据串联
- **禁止硬编码权限标识** -- 权限标识应来自数据库或配置，不要在代码中写死权限字符串进行判断
- **禁止绕过框架直接操作 SA-Token 内部 API**（如 `getSaTokenDao()`、`deleteTokenToIdMapping()`）-- 这些与 SA-Token 版本强耦合，升级时可能中断
- **禁止在白名单路径中使用过于宽泛的匹配模式**（如 `/**`）-- 会导致所有接口跳过认证，存在安全风险
- **禁止忽略反向权限模型的语义** -- CDP 存储的是用户"无权限"的资源列表，`StpUtil.hasPermission(uri)` 返回 `true` 表示该 URI 存在于禁止列表中（即无权限），与 SA-Token 原生语义相反
