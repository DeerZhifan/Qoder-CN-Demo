# CDP 认证授权模块设计手册

> 对应使用手册：[cdp-module-auth.md](../3-组件使用手册/cdp-module-auth.md)

## 一、设计目标与背景

认证授权是企业级应用的核心横切关注点。直接使用 SA-Token 原生 API 散落在业务代码中会带来以下问题：

1. **耦合度高** — 业务代码直接调用 `StpUtil` 静态方法，无法在不同认证方案间切换，也难以进行单元测试。
2. **扩展性差** — 原生拦截器无法优雅地接入多种认证方式（JWT、API Key、CAS 等），每新增一种方式需修改核心认证逻辑。
3. **上下文管理复杂** — Token 解析、用户信息、租户 ID 需要在请求生命周期内传递，手动管理 ThreadLocal 容易遗漏清理导致内存泄漏或数据串联。
4. **权限模型不统一** — SA-Token 默认采用"拥有权限"正向模型，而 CDP 基于业务需求采用"反向权限"模型（存储无权限列表），需要适配层桥接。

CDP 认证模块的设计原则：

- **面向接口编程** — 通过 `IUserHelper` 接口抽象用户信息获取，业务代码不直接依赖 SA-Token。
- **可插拔认证链** — 通过 `AuthHandlerExecutor` Strategy 接口支持多种认证方式并行，新增认证方式只需实现接口并注册 Bean。
- **请求级上下文自动管理** — 拦截器自动完成 ThreadLocal 的设置与清理，业务代码无需关心生命周期。
- **自动装配** — 通过 `@AutoConfiguration` 和条件注解实现零配置接入，同时允许覆盖任何默认行为。

## 二、整体架构

```
┌─────────────────────── HTTP 请求入口 ────────────────────────┐
│                                                               │
│  Filter Chain: MDCRequestFilter → CheckCSRFFilter →           │
│                RepeatedlyRequestFilter → BaseRequestFilter     │
│                                                               │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              CdpSaInterceptor (HandlerInterceptor)       │  │
│  │  preHandle:                                              │  │
│  │    1. CdpTokenHolder.clear() / TenantContentHolder.clear │  │
│  │    2. 检测微服务内部调用 Header                            │  │
│  │    3. 委托 CdpAuthHandler.checkAuthAndPermission(uri)     │  │
│  │    4. 解析 Token → BaseUserInfo → CdpTokenHolder          │  │
│  │    5. 设置 TenantContentHolder                            │  │
│  │  afterCompletion:                                        │  │
│  │    清除 CdpTokenHolder / TenantContentHolder              │  │
│  └──────────────────────────┬──────────────────────────────┘  │
│                             ▼                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              CdpAuthHandler (认证编排器)                    │ │
│  │                                                          │ │
│  │  1. 遍历 AuthHandlerExecutor 链                           │ │
│  │  2. 登出 URL 特殊处理                                     │ │
│  │  3. 白名单匹配（isIgnoreToken）                           │ │
│  │  4. StpUtil.isLogin() 登录校验                            │ │
│  │  5. 反向权限校验（StpUtil.hasPermission）                  │ │
│  │  6. Token 续期（refreshTokenTime）                        │ │
│  └────────┬─────────────────────────────────────────────────┘ │
│           ▼                                                   │
│  ┌────────────────────┐  ┌───────────────────────────────┐   │
│  │ AuthHandlerExecutor │  │ AuthHandlerExecutor            │   │
│  │ (如 ApiKey 认证)    │  │ (其他自定义认证)                │   │
│  └────────────────────┘  └───────────────────────────────┘   │
│                                                               │
├───────────────────────── SA-Token 层 ────────────────────────┤
│  ┌──────────────────┐  ┌─────────────────┐                   │
│  │ StpLogicJwtForMixin│  │ CdpStpInterface │                  │
│  │ (JWT 混合模式)     │  │ (反向权限适配)   │                  │
│  └──────────────────┘  └─────────────────┘                   │
│                                                               │
├──────────────────── ThreadLocal 上下文 ──────────────────────┤
│  CdpTokenHolder (Token + BaseUserInfo)                        │
│  TenantContentHolder (租户 ID + 忽略表)                        │
└───────────────────────────────────────────────────────────────┘
```

## 三、核心设计模式

### 3.1 Strategy 模式 — AuthHandlerExecutor

`AuthHandlerExecutor` 定义了可插拔的认证策略接口，每个实现类封装一种独立的认证方式。

**参与类：**
- **Strategy 接口**：`AuthHandlerExecutor` — 单方法接口 `checkAuthAndPermission(String uri)`，返回 `true` 表示该策略已处理认证（短路后续逻辑），返回 `false` 表示不适用。
- **Context**：`CdpAuthHandler` — 持有 `List<AuthHandlerExecutor>`，遍历执行。
- **具体策略**：`ApiKeyAuthHandlerExecutor`（API Key 认证，位于 `leatop-cdp-base-apikey` 模块）。

> 设计决策：采用 Strategy + 短路遍历而非传统的 Chain of Responsibility，是因为各认证方式之间是互斥关系（一个请求只会匹配一种认证方式），不需要链式传递。返回布尔值的设计使语义更清晰：`true` = "我已处理"，`false` = "不是我的职责"。

### 3.2 Adapter 模式 — CdpStpInterface

CDP 采用"反向权限"模型（存储用户无权限的资源列表），而 SA-Token 的 `StpInterface.getPermissionList()` 语义是"拥有的权限列表"。CdpStpInterface 通过适配器桥接这一语义差异。

**参与类：**
- **Target**：SA-Token 的 `StpInterface`
- **Adapter**：`CdpStpInterface`
- **Adaptee**：`IUserHelper.getForbiddenPermissions()`

CdpStpInterface 将 `getPermissionList()` 的返回值映射为"禁止权限列表"，配合 `CdpAuthHandler` 中的 `StpUtil.hasPermission(uri)` 判断（URI 存在于列表中则无权限），实现了反向权限语义。

> 设计决策：选择"反向权限"模型是因为 CDP 面向的企业场景中，大多数资源对大多数用户开放，仅少数资源受限。存储"无权限列表"比存储"有权限列表"数据量更小，权限变更时影响面也更小。

### 3.3 模板方法 + 拦截器模式 — CdpSaInterceptor

CdpSaInterceptor 实现 Spring `HandlerInterceptor`，在 `preHandle` 中编排认证流程，在 `afterCompletion` 中清理上下文。

**参与类：**
- `CdpSaInterceptor` — 拦截器本体，负责请求生命周期管理
- `CdpAuthHandler` — 被委托执行具体认证逻辑
- `CdpTokenHolder`、`TenantContentHolder` — ThreadLocal 上下文持有者

> 设计决策：没有直接使用 SA-Token 的 `SaInterceptor`，而是自定义 `CdpSaInterceptor`。原因是 CDP 需要额外处理微服务内部调用（通过 `CDP-MICRO-TOKEN` Header 识别）、Token 解析后填充 `BaseUserInfo`、以及租户上下文设置等逻辑，这些超出了 SA-Token 原生拦截器的职责。

### 3.4 条件装配模式 — CdpAuthAutoConfiguration

通过 Spring Boot 的 `@ConditionalOnMissingBean` 和 `@ConditionalOnClass` 注解实现默认行为可覆盖。

**参与类：**
- `CdpAuthAutoConfiguration` — 自动配置入口
- `CdpTokenConfigure` — SA-Token 参数配置
- `CdpWebConfigurer` — 拦截器注册

## 四、关键类说明

| 类名 | 包 | 职责 | 设计角色 |
|------|-----|------|---------|
| `CdpAuthAutoConfiguration` | `com.leatop.cdp.auth` | 自动配置入口：注册 StpInterface、StpLogic（JWT 混合模式）、CdpSaInterceptor | 自动配置 |
| `CdpAuthHandler` | `com.leatop.cdp.auth.api` | 认证编排器：协调 AuthHandlerExecutor 链、白名单判断、登录校验、权限校验、Token 续期 | Strategy Context |
| `AuthHandlerExecutor` | `com.leatop.cdp.auth.api` | 可插拔认证处理器接口，返回 boolean 表示是否已处理 | Strategy 接口 |
| `SysUserHelper` | `com.leatop.cdp.auth.api` | IUserHelper 默认实现，基于 SA-Token Session 管理用户信息和权限 | 接口实现 |
| `CdpStpInterface` | `com.leatop.cdp.auth.handler` | 适配 SA-Token 权限接口，将 IUserHelper 的反向权限列表桥接到 StpInterface | Adapter |
| `CdpSaInterceptor` | `com.leatop.cdp.auth.interceptor` | 请求拦截器：上下文初始化/清理、微服务内部调用识别、委托认证、填充用户和租户上下文 | Interceptor |
| `CdpIgnoreProperties` | `com.leatop.cdp.auth.properties` | 白名单配置：baseWhiteList（内置）、authWhiteList（自定义）、whitePermission（权限白名单）| 配置承载 |
| `CdpTokenConfigure` | `com.leatop.cdp.auth.config` | 通过 @Value 注入 SA-Token 配置参数并以代码方式设置到 SaTokenConfig | 配置桥接 |
| `CdpWebConfigurer` | `com.leatop.cdp.auth.config` | WebMvcConfigurer 实现，注册 CdpSaInterceptor 并设置排除路径 | 拦截器注册 |
| `Slf4jSaLog` | `com.leatop.cdp.auth.log` | 将 SA-Token 内部日志桥接到 SLF4J | 日志适配器 |

## 五、扩展机制

### 5.1 自定义认证方式 — 实现 AuthHandlerExecutor

新增认证方式只需实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean：

```java
@Component
public class OAuth2AuthHandlerExecutor implements AuthHandlerExecutor {
    @Override
    public boolean checkAuthAndPermission(String uri) {
        // 检测请求中是否携带 OAuth2 Bearer Token
        // 如果是 OAuth2 请求，完成认证并返回 true
        // 如果不是，返回false，交给下一个处理器
        return false;
    }
}
```

CdpAuthHandler 通过 Spring 依赖注入自动收集所有 `AuthHandlerExecutor` Bean，无需手动注册。已有示例：`ApiKeyAuthHandlerExecutor`（位于 `leatop-cdp-base-apikey` 模块）。

### 5.2 替换权限加载策略 — 实现 PermissionManage

SysUserHelper 中通过 `@Autowired(required = false)` 注入 `PermissionManage`。当该 Bean 存在时，权限列表从 `PermissionManage.getUserForbiddenPermissions()` 实时加载；不存在时回退到 SA-Token Session 中的缓存数据。

### 5.3 覆盖 StpInterface

CdpAuthAutoConfiguration 中 StpInterface Bean 标注了 `@ConditionalOnMissingBean`，业务方可提供完全自定义的权限加载实现。

### 5.4 覆盖 StpLogic

JWT 模式的 `StpLogic` Bean 同样标注了 `@ConditionalOnMissingBean`，可替换为 SA-Token 的其他模式（如 Simple 模式、Stateless 模式）。

### 5.5 白名单配置扩展

`CdpIgnoreProperties` 绑定 `cdp.security` 前缀，支持通过 YAML 配置 `auth-white-list` 和 `white-permission` 追加自定义白名单路径，与内置的 `baseWhiteList` 合并生效。

## 六、模块协作

```
leatop-cdp-common-auth
    │
    ├── 依赖 ──► leatop-cdp-common-core
    │              ├── IUserHelper (接口定义)
    │              ├── PermissionManage (接口定义)
    │              ├── CdpTokenHolder (ThreadLocal 上下文)
    │              ├── TenantContentHolder (租户上下文)
    │              └── AntPathMatcherUtils (路径匹配)
    │
    ├── 依赖 ──► leatop-cdp-common-data
    │              ├── BaseUserInfo (用户基本信息 DTO)
    │              ├── CurrentUserDto (完整用户信息 DTO)
    │              ├── UnauthorizedException / UncheckedException
    │              ├── ErrorCodeEnum
    │              └── HeaderConstant (微服务 Header 常量)
    │
    ├── 依赖 ──► SA-Token (cn.dev33.satoken)
    │              ├── StpUtil, StpLogic, StpInterface
    │              ├── StpLogicJwtForMixin (JWT 混合模式)
    │              ├── SaSession (Token Session)
    │              └── SaManager, SaTokenConfig
    │
    ├── 被扩展 ◄── leatop-cdp-base-apikey
    │              └── ApiKeyAuthHandlerExecutor (AuthHandlerExecutor 实现)
    │
    ├── 被依赖 ◄── leatop-cdp-business-system-login (登录流程调用 IUserHelper)
    └── 被依赖 ◄── 所有需要认证的业务模块 (通过 IUserHelper 获取用户信息)
```

**请求处理数据流：**

1. HTTP 请求经过 Filter Chain 后进入 `CdpSaInterceptor.preHandle()`。
2. 拦截器清除上一请求的 ThreadLocal 残留，检测是否为微服务内部调用。
3. 非内部调用时委托 `CdpAuthHandler.checkAuthAndPermission(uri)`。
4. CdpAuthHandler 依次：遍历 AuthHandlerExecutor 链 → 检查登出 URL → 匹配白名单 → 校验登录 → 校验权限 → 刷新 Token 活跃时间。
5. 认证通过后，拦截器调用 `CdpAuthHandler.parseToken()` 从 JWT 提取用户信息，写入 `CdpTokenHolder` 和 `TenantContentHolder`。
6. 业务 Controller 通过 `IUserHelper`（实际为 SysUserHelper）读取当前用户信息，SysUserHelper 优先从 CdpTokenHolder 读取，回退到 SA-Token Session。
7. 请求完成后，`afterCompletion()` 清除所有 ThreadLocal。

## 七、设计权衡与约束

### 7.1 显式权衡

**反向权限 vs 正向权限**：CDP 存储"用户无权限的资源列表"而非"用户拥有的权限列表"。在资源数量远大于受限资源的场景下，反向模型存储更高效。但这与 SA-Token 的正向语义冲突，需要 CdpStpInterface 做适配，且 `StpUtil.hasPermission(uri)` 的语义被反转（返回 true 表示"存在于禁止列表中"即无权限），增加了理解成本。

> 设计决策：接受语义反转的复杂度，因为反向模型更贴合实际业务场景（大部分资源默认开放），且在 CdpAuthHandler 中集中处理了反转逻辑，业务开发者通过 IUserHelper 接口无需感知底层权限模型。

**拦截器 vs Filter**：选择 Spring MVC `HandlerInterceptor` 而非 Servlet `Filter` 实现认证。

> 设计决策：拦截器工作在 DispatcherServlet 之后，能够获取到 Handler 信息（Controller 方法），且通过 `CdpWebConfigurer` 可以利用 Spring 的路径匹配机制精确排除白名单路径。同时 `@ConditionalOnMissingClass("SaReactorFilter")` 确保在 WebFlux（网关）环境下不会注册 MVC 拦截器。

**代码配置 vs YAML 配置**：`CdpTokenConfigure` 通过 `@Value` 读取配置后以代码方式设置到 `SaTokenConfig`，而非完全依赖 SA-Token 的 YAML 自动绑定。

> 设计决策：代码配置优先级高于 YAML，确保框架的默认值（如 `token-name: cdp-token`、`active-timeout: 36000`）不会被误覆盖。同时保留了 YAML 可配置性作为补充。

### 7.2 已知限制

- **AuthHandlerExecutor 无序**：当前 `List<AuthHandlerExecutor>` 的遍历顺序取决于 Spring Bean 注册顺序，没有显式的优先级控制（如 `@Order`）。如果多个 Executor 可能同时匹配同一请求，结果不确定。
- **Token 续期竞态**：`refreshTokenTime()` 中的超时判断和续期操作不是原子的，在极高并发下可能出现不必要的重复续期，但不影响正确性。
- **SysUserHelper 多次 Session 查询**：`getCurrentUserId()` 方法在 CdpTokenHolder 中未找到用户 ID 时会调用 `getCurrentUserInfo()` 触发一次完整的 Session 查询，在同一请求中可能重复执行。
- **登出流程耦合**：`logoutByTokenValue()` 中针对 `StpLogicJwtForMixin` 的特殊处理直接操作了 SA-Token 内部 API（`getSaTokenDao()`、`deleteTokenToIdMapping()` 等），与 SA-Token 版本强耦合。

### 7.3 演进方向

- 为 `AuthHandlerExecutor` 增加 `@Order` 或 `getOrder()` 方法，支持显式优先级排序。
- 启用被注释掉的 API Key 认证逻辑（当前 `CdpAuthHandler` 中有 TODO 注释），与 `ApiKeyAuthHandlerExecutor` 的 Strategy 实现统一。
- 引入请求级缓存，避免 SysUserHelper 在同一请求中重复查询 SA-Token Session。
- 考虑将 `CdpTokenConstant`（当前被注释掉）恢复并统一微服务 Header 常量定义。
