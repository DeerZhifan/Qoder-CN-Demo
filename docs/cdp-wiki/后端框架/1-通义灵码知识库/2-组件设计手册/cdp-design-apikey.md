# CDP API Key 认证 设计手册

> 对应使用手册：[cdp-module-apikey.md](../3-组件使用手册/cdp-module-apikey.md)

## 一、设计目标与背景

API Key 认证组件（`leatop-cdp-base-apikey`）为 CDP 框架提供面向第三方系统的接口认证能力。在 SA-Token 的 API Key 机制之上，CDP 扩展了以下特性：

1. **双认证策略** -- 同时支持 API Key 直传和 SaSign 签名认证两种方式，通过责任链模式依次检查。
2. **与 auth 模块统一集成** -- 认证处理器实现 `AuthHandlerExecutor` 接口，融入 CDP 统一认证链，认证通过后自动填充 `CdpTokenHolder` 上下文。
3. **双存储源** -- API Key 同时支持配置文件声明和数据库动态管理，应用启动时预加载配置文件中的 Key。

> 设计决策：选择 SA-Token 的 API Key 模块而非自研，利用其成熟的 Key 生命周期管理和缓存机制，CDP 专注于认证链集成和 scope 匹配逻辑。

## 二、整体架构

```
┌─────────────────────────────────────────────────┐
│                  HTTP 请求                       │
└───────────┬─────────────────────────────────────┘
            │ CDP 认证过滤器
            v
┌─────────────────────────────────────────────────┐
│            AuthHandlerExecutor 责任链             │
│  ┌───────────────────────────┐                  │
│  │ ApiKeyAuthHandlerExecutor │ ← API Key 认证   │
│  └─────────┬─────────────────┘                  │
│            │ 未匹配                              │
│  ┌─────────v─────────────────┐                  │
│  │ SaSignAuthHandlerExecutor │ ← 签名认证        │
│  └─────────┬─────────────────┘                  │
│            │ 未匹配                              │
│            v 其他认证处理器...                     │
└─────────────────────────────────────────────────┘
            │ 认证通过
            v
┌─────────────────────────────────────────────────┐
│        CdpTokenHolder.setUserInfo()             │
│        写入用户上下文，后续业务可获取              │
└─────────────────────────────────────────────────┘
```

**数据流**：

```
ApiManagerService (ApplicationRunner)
  ├── 启动时加载 ApiKeyProperties (YAML)
  ├── 调用 SaApiKeyUtil.saveApiKey() 写入 SA-Token 缓存
  └── 运行时 getApiKeyModelFromDatabase() 查询数据库
```

## 三、核心设计模式

### 策略模式 -- AuthHandlerExecutor

`AuthHandlerExecutor` 是 CDP 认证模块（`leatop-cdp-common-auth`）定义的扩展接口，方法签名为 `boolean checkAuthAndPermission(String uri)`。API Key 模块提供两个实现：

- `ApiKeyAuthHandlerExecutor`：从请求中读取 `apiKey` 参数/头，调用 SA-Token 校验 Key 有效性，再用 `AntPathMatcherUtils` 匹配 scope。
- `SaSignAuthHandlerExecutor`：从 `Authorization: SaSign ...` 头中解析签名参数，通过 `SaSignTemplate` 校验 SHA256 签名。

两者均返回 `boolean`：返回 `true` 表示已处理认证（成功或抛出异常），返回 `false` 表示当前请求不属于该认证方式，交由下一个处理器。

> 设计决策：每个 Executor 独立判断自身是否适用，而非由统一调度器分发，降低了各认证策略之间的耦合。

### 双源数据加载

`ApiManagerService` 同时实现 `SaApiKeyDataLoader`（SA-Token SPI）和 `ApplicationRunner`：

- **启动加载**：`run()` 方法遍历 `ApiKeyProperties.apiKeys`（YAML 配置），逐个写入 SA-Token 缓存。
- **运行时查询**：`getApiKeyModelFromDatabase()` 通过 `ApiKeyDAO` 从 `frame_api_keys` 表查询，优先级高于缓存中的配置数据。

### Scope 匹配

API Key 的 `scopes` 字段存储 Ant 风格路径列表（如 `/api/users/**`）。`ApiKeyAuthHandlerExecutor` 在校验 Key 有效性后，遍历 scopes 列表，使用 `AntPathMatcherUtils.match()` 逐一匹配请求 URI。任一 scope 匹配即放行，全部不匹配则抛出 `ApiKeyScopeException`。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `CdpApiKeyAutoConfiguration` | 自动配置类，扫描组件包和 Mapper |
| `ApiKeyAuthHandlerExecutor` | API Key 认证处理器，校验 Key 有效性和 scope 范围 |
| `SaSignAuthHandlerExecutor` | SaSign 签名认证处理器，校验 SHA256 签名 |
| `ApiManagerService` | API Key 数据加载器，负责启动预加载和数据库查询 |
| `ApiKeyProperties` | 绑定 `api-manage.api-keys` YAML 配置 |
| `ApiKeyController` | REST 接口，提供 Key 的 CRUD 操作 |
| `ApiKeyDAO` | MyBatis-Plus Mapper，操作 `frame_api_keys` 表 |
| `ApiKeyPO` / `ApiKeyDTO` | 数据库实体和传输对象 |

## 五、扩展机制

1. **新增认证策略**：实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean，即可加入 CDP 认证责任链，无需修改现有代码。
2. **自定义 Key 生成规则**：通过 SA-Token 的 `SaApiKeyManager.getSaApiKeyTemplate()` 自定义 Key 值生成算法。
3. **外部 Key 存储**：重写 `ApiManagerService.getApiKeyModelFromDatabase()` 可对接外部 Key 管理系统（如 API Gateway）。

## 六、模块协作（简要）

- **auth 模块**：`AuthHandlerExecutor` 接口定义在 `leatop-cdp-common-auth` 中，apikey 模块实现该接口融入统一认证链。
- **system 模块**：认证通过后通过 `UserBusiness.get()` 查询用户详情填充 `BaseUserInfo`，实现 Key 与用户身份的绑定。
- **SA-Token**：底层依赖 SA-Token 1.44.0 的 `sa-token-apikey` 和 `sa-token-sign` 模块，CDP 不直接操作 Token 存储。

## 七、设计权衡与约束（简要）

- **配置文件 Key 不可动态删除**：YAML 中声明的 Key 在启动时写入缓存，运行时删除需通过 `updateAllApiKeys()` 全量刷新。
- **SaSign 的 secretKey 存储**：当前复用 `ApiKeyModel.title` 字段存储 secretKey，语义不够清晰，后续计划迁移到专用字段。
- **Scope 仅支持路径匹配**：不支持 HTTP 方法级别的权限控制，如需区分 GET/POST 需在业务层自行判断。
