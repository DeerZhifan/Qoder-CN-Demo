# CDP 登录认证模块设计手册

> **源码位置：** `leatop-cdp-business/leatop-cdp-business-system/leatop-cdp-business-system-login/`
> **通用架构模式：** 参见 [cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)
> **对应使用手册：** `doc/cdp-module-system-login.md`

---

## 一、模块定位

login 模块是 system 业务域中一个**独立的子模块**，与标准的 5 子模块结构不同，它没有单独的 api / service / controller 拆分，而是作为一个自包含的功能单元直接嵌入 system 模块。其核心职责包括：

- 提供多种登录方式的统一入口
- 管理 Token 的生成、存储与销毁
- 支持安全策略（密码强度校验、账号锁定、强制改密）
- 发布登录事件供下游监听
- 与 auth、cache、message 等模块协作

> 设计决策：login 不遵循标准 5 子模块拆分，是因为登录功能与 system 模块中的用户（User）、安全设置（SecuritySetting）紧密耦合，且不需要被其他业务模块通过 Feign 远程调用，独立为子模块已足够隔离关注点。

---

## 二、核心类图

```
LoginController                            CasProxyController
      |                                          |
      v                                          v
  LoginService (接口)                    CdpCasServerMonitor
      |                                    (CAS 服务健康监控)
      v
  LoginServiceImpl
      |
      +---> UserBusiness (@FeignClient)      -- 用户查询与信息获取
      +---> SecuritySettingBusiness          -- 安全策略读取
      +---> IUserHelper                      -- 会话管理（set/get 当前用户）
      +---> AccessTokenService (SPI)         -- 第三方 Token 校验
      +---> ApplicationEventPublisher        -- 登录事件发布
      +---> CdpCacheClient                   -- 验证码与临时凭证缓存
      +---> MessageBusiness                  -- 短信/邮件验证码发送
      +---> UserExtraDataService (SPI)       -- 登录后用户扩展数据注入
      +---> PermissionManage (SPI)           -- 权限管理（可选）
```

---

## 三、多登录模式设计

### 3.1 模式枚举

`LoginModeEnum` 定义了三种登录模式：

| 枚举值 | type | 说明 |
|--------|------|------|
| PWD | 0 | 账号密码登录 |
| MOBILE | 1 | 手机短信验证码登录 |
| EMAIL | 2 | 邮箱验证码登录 |

前端通过 `LoginUserDto.loginMode` 字段传入模式标识，`LoginServiceImpl.webLogin()` 依据该值进入不同的校验分支。

### 3.2 分支路由而非 Strategy 模式

当前实现中，不同登录模式并未抽象为独立的 Strategy 类，而是在 `LoginServiceImpl.webLogin()` 方法内部通过条件分支处理：

- **PWD 模式**：解密前端密码（支持 AES / RSA 两种加密算法，由 `LoginUserDto.alg` 字段指定）-> `CdpPasswordEncoder.matches()` 校验密码 -> 失败时记录错误次数并判断账号锁定。
- **MOBILE / EMAIL 模式**：从 `CdpCacheClient` 中读取先前缓存的验证码 -> 比对用户输入 -> 校验通过后删除缓存防止重复使用。

用户查询同样依据模式路由，在 `queryUserInfo()` 私有方法中通过 `switch` 分别调用 `UserBusiness.getUserByAccountAndTenantIdForLogin()`、`getUserByMobileAndTenantIdForLogin()` 或 `queryUserInfo()`。

> 设计决策：当前使用条件分支而非 Strategy 模式，原因是登录模式仅三种且流程差异集中在"凭证校验"一步，其余流程（安全策略检查、Token 生成、事件发布）完全共享。若未来新增更多登录模式（如指纹、扫码），建议重构为 Strategy 模式。

### 3.3 CAS 单点登录

CAS 登录由两个独立的 Controller 协作完成：

- **CasProxyController** -- 负责将浏览器重定向到 CAS Server 登录页（`/cas/login`）和登出页（`/cas/logout`）。它通过 `CdpCasServerMonitor` 获取当前活跃的 CAS 服务实例地址，支持多 CAS Server 高可用。
- **LoginController** -- 提供 CAS 回调接口 `casCallBack` 和 `casLogin`，分别处理 GET 方式的页面重定向回调和 POST 方式的 API 登录。

CAS 登录流程：

1. 前端调用 `/cas/login` -> `CasProxyController` 拼接 `service` 参数 -> 302 重定向到 CAS Server。
2. 用户在 CAS Server 完成认证 -> CAS Server 回调 `/login/casCallBack` 并携带 `ticket`。
3. `LoginServiceImpl.casCallBack()` 将 ticket 拼接到前端页面地址，302 重定向到前端 CAS 登录页。
4. 前端拿到 ticket 后调用 `/login/casLogin` -> `LoginServiceImpl.casLogin()` 通过 HTTP 调用 CAS Server 的 `/serviceValidate` 接口验证 ticket -> 获取用户名 -> 查询本地用户 -> 调用 `saTokenLogin()` 生成 Token。
5. ticket 与 token 的映射关系缓存到 Redis（TTL 7 天），供后续 CAS 登出回调使用。

### 3.4 AccessToken 第三方登录

`AccessTokenService` 是一个 SPI 扩展点。默认实现 `DefaultAccessTokenServiceImpl` 对接"粤交通"平台，通过 HTTP POST 调用外部接口验证 accessToken 并获取用户账号。

`CdpLoginConfig` 中使用 `@ConditionalOnMissingBean` 注册默认实现，应用可以提供自定义实现来覆盖，从而对接任意第三方认证平台。

---

## 四、Token 生成与管理

### 4.1 SA-Token 登录

所有登录方式最终汇聚到 `LoginServiceImpl.saTokenLogin()` 私有方法，该方法完成以下操作：

1. 构造 `SaLoginParameter`，设置设备类型（PC / APP，由 User-Agent 判断）。
2. 若 SA-Token 支持 Extra 扩展（`StpLogic.isSupportExtra()`），则将 `userName`、`account`、`orgId`、`tenantId` 写入 Token Extra。
3. 调用 `StpUtil.login(userId, saLoginModel)` 完成登录。若因 SA-Token 版本升级导致 session 缓存格式不兼容（`SaJsonConvertException`），则自动删除旧 session 并重试。
4. 通过 `UserBusiness.getUserByIdAndOrgId()` 获取完整用户信息 `CurrentUserDto`。
5. 若未注入 `PermissionManage`，则通过 `UserBusiness.getUserForbiddenPermissions()` 获取用户禁止权限列表并写入 session。
6. 遍历所有 `UserExtraDataService` 实现，收集扩展数据并附加到 `CurrentUserDto.extraData`。
7. 调用 `IUserHelper.setCurrentUserInfo()` 将用户信息存入 SA-Token session。
8. 返回 `StpUtil.getTokenValue()`。

### 4.2 LoginResultDto 状态码机制

`LoginResultDto.code` 字段用于指示前端的下一步行为：

| code | 含义 | 前端处理 |
|------|------|----------|
| 200 | 登录成功 | 跳转系统首页 |
| 10010 | 需修改密码 | 弹出改密界面，调用 `/login/changePwd` |
| 10020 | 需二次登录 | 展示第二种登录方式验证界面 |
| 10030 | 登录信息失效 | 重新回到登录页 |

> 设计决策：使用业务状态码而非 HTTP 状态码区分登录流程分支，保证所有响应 HTTP 200，由前端根据 `code` 决定路由，避免浏览器或网关对非 200 响应的特殊处理干扰。

---

## 五、安全策略执行

登录过程中，`LoginServiceImpl` 通过 `SecuritySettingBusiness.getEnableSecuritySettingValues()` 获取当前生效的安全配置 `SecuritySettingValuesDto`，并在以下环节执行安全策略：

### 5.1 验证码校验

当安全配置 `hasVerificationCode` 为 true 且 `CaptchaProperties.useCaptcha` 为 true 时，PWD 模式要求图形验证码。验证码通过 `createLoginCode()` 生成，存入 `CdpCacheClient` 缓存（key = `captcha_codes:` + UUID），前端提交时携带 `key` 和 `code` 进行比对。

验证码类型由 `CaptchaProperties.CaptchaType` 枚举控制，支持 GIF 动图、PNG 静图、中文、中文动图、算术五种样式。

### 5.2 账号锁定

当安全配置 `hasPasswordFailedLock` 为 true 时，连续登录失败达到 `accountLockCount` 次后，账号被锁定 `accountLockTimes` 小时。锁定信息记录在用户表的 `locktime` 和 `loginerrorcount` 字段。登录时检查锁定状态，未解锁则直接拒绝。

### 5.3 密码强度与过期

`checkPassword()` 方法在密码登录成功后执行以下检查：

- 是否使用系统初始密码（`CoreConstants.DEFAULT_PWD`）
- 是否为管理员重置后的密码（`hasFirstLoginReset` + `OperateSource.RESET_PASSWORD`）
- 密码是否过期（`hasPasswordExpiredDays` + 天数比较）
- 密码强度（最小长度、大小写字母、数字、特殊字符），由 `PasswordStrengthChecker` 工具类逐项检查

任一检查不通过，返回 code=10010，要求前端引导改密。

### 5.4 二次登录

当安全配置 `hasShortMessageLogin` 为 true 时，启用双因素认证。第一次登录成功后，返回 code=10020 和一个临时 `preLoginToken`（UUID），对应的 SA-Token 存入缓存（TTL 10 分钟）。前端引导用户使用第二种登录方式（如第一次用密码则第二次用短信），并回传 `preLoginToken`。`webLogin()` 检测到 `preLoginToken` 后，验证两次不是同一模式、且前一次登录未过期，然后完成最终认证。

---

## 六、登录事件发布

登录成功后，`LoginServiceImpl` 通过 Spring `ApplicationEventPublisher` 发布 `LoginSuccessEvent`，携带 `userId`。

`LoginSuccessEvent` 定义在 `leatop-cdp-common-data` 模块中（`com.leatop.cdp.data.event.LoginSuccessEvent`），继承 `ApplicationEvent`，属于框架级公共事件。

应用方通过实现 `ApplicationListener<LoginSuccessEvent>` 或使用 `@EventListener` 注解即可监听此事件，典型用途包括：初始化用户缓存数据、记录登录统计、推送欢迎通知等。示例可参见 `leatop-cdp-example-demo1` 中的 `LoginSuccessEventListener`。

> 设计决策：使用 Spring 事件机制而非直接调用，是为了保持 login 模块与下游逻辑的松耦合。login 模块不需要知道有哪些监听者，新增监听者也无需修改 login 代码。

---

## 七、与 auth 模块的协作

login 模块与 `leatop-cdp-common-auth` 模块通过 `IUserHelper` 接口进行协作。`IUserHelper` 定义在 `leatop-cdp-common-core` 中，由 auth 模块基于 SA-Token 提供实现。login 模块依赖此接口完成以下操作：

| 调用方法 | 调用时机 | 作用 |
|----------|----------|------|
| `setCurrentUserInfo(CurrentUserDto)` | `saTokenLogin()` 中 Token 生成后 | 将用户信息写入 SA-Token session |
| `setForbiddenPermissions(List<String>)` | `saTokenLogin()` 中 Token 生成后 | 将禁止权限列表写入 session |
| `getCurrentUserInfo()` | `webLogout()` 中退出前 | 获取当前用户用于记录退出日志 |
| `getCurrentUserInfoByToken(String)` | 二次登录和改密流程中 | 根据临时 token 获取用户信息 |
| `isLogin()` | `webLogout()` | 判断当前是否已登录 |
| `logout()` | `webLogout()` | 清除 SA-Token session 和 token |
| `logoutByTokenValue(String)` | CAS 登出回调 | 根据特定 token 值登出 |

> 设计决策：login 模块依赖 `IUserHelper` 接口而非直接调用 SA-Token API，使得认证框架可替换。若未来从 SA-Token 迁移到其他方案，只需替换 `IUserHelper` 实现，login 模块代码无需修改。

---

## 八、SPI 扩展点

login 模块提供三个关键的 SPI 扩展点，均通过 Spring 条件注入实现：

### 8.1 AccessTokenService

用于第三方 Token 校验。`CdpLoginConfig` 通过 `@ConditionalOnMissingBean` 注册默认实现 `DefaultAccessTokenServiceImpl`。应用在自己的配置中声明一个 `AccessTokenService` Bean 即可替换。

### 8.2 UserExtraDataService

用于在登录成功后向 session 注入扩展数据。支持多实现（`List<UserExtraDataService>`），各实现返回的 `Map<String, Object>` 会合并到 `CurrentUserDto.extraData` 中。典型场景：注入用户的组织树路径、自定义偏好设置等。

### 8.3 PermissionManage

用于权限管理策略替换。当应用注入了 `PermissionManage` 实现时，login 模块跳过默认的 `UserBusiness.getUserForbiddenPermissions()` 调用，改由 `PermissionManage` 接管权限加载逻辑。

---

## 九、CAS 高可用监控

`CdpCasServerMonitor` 组件在应用启动时创建定时线程池，按照 `cdp.cas.check-interval`（默认 5 秒）周期性检查所有配置的 CAS Server 健康状态。检查方式为 HTTP GET 请求 CAS Server 的 `/login` 页面，超时 1 秒。

活跃服务列表序列化为 JSON 存入 `CdpCacheClient`（缓存名 `cdp:active_cas_service`），所有应用实例共享同一份活跃列表，避免重复探测。`getActiveServer()` 方法在 CAS 登录和登出时被调用，自动选取可用的 CAS 实例。

> 设计决策：CAS 健康检查结果存入分布式缓存而非本地变量，是为了在集群部署时所有节点共享探测结果，减少对 CAS Server 的探测压力。同时通过 `last_check_time` 机制防止多节点并发探测。
