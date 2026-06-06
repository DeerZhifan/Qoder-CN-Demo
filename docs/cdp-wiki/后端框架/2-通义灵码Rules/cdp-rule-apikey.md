---
trigger: when_referenced
knowledge_source:
  - cdp-design-apikey
  - cdp-module-apikey
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-apikey` 依赖
- 激活 `apikey` profile
- 使用 `SaApiKeyUtil` 创建或校验 API Key
- 配置 `api-manage.api-keys` YAML 属性
- 操作 `frame_api_keys` 数据库表
- 使用 `SaSign` 签名认证方式

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-apikey</artifactId>
</dependency>
```

2. 激活 apikey profile：

```yaml
spring:
  profiles:
    active: apikey
```

3. 可选：创建 `frame_api_keys` 数据库表（支持数据库动态管理 API Key）。

---

## 配置要点

### 通过配置文件定义 API Key

```yaml
api-manage:
  api-keys:
    - title: "第三方系统A"
      intro: "用于系统A的数据同步接口"
      apiKey: "your-api-key-value"
      loginId: "user-123"
      createTime: 1706355600000
      expiresTime: -1              # -1 表示永不过期
      isValid: true
      scopes:                      # 允许访问的 API 路径（Ant 风格）
        - "/api/users/**"
        - "/api/products/**"
```

### 双认证方式

- **API Key 认证**：请求头或参数携带 `apiKey`，框架自动校验有效性和 scope 范围。
- **SaSign 签名认证**：请求头携带 `Authorization: SaSign ...`，包含 appid、timestamp、nonce、sign 参数，使用 SHA256 签名。

### 双存储源

API Key 同时支持配置文件声明和数据库动态管理。启动时预加载 YAML 配置中的 Key，运行时数据库查询优先级高于缓存中的配置数据。

### 认证链集成

`ApiKeyAuthHandlerExecutor` 和 `SaSignAuthHandlerExecutor` 实现 `AuthHandlerExecutor` 接口，融入 CDP 统一认证责任链。认证通过后自动填充 `CdpTokenHolder` 上下文。

### 调用方传递 API Key 的方式

```bash
# 方式1：请求参数
GET /api/users/list?apiKey=your-api-key-value

# 方式2：请求头
GET /api/users/list
Header: apiKey: your-api-key-value
```

### 内置 REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api_access/get_api_keys` | GET | 查询所有 API Key |
| `/api_access/save_api_keys` | POST | 创建 API Key（自动生成 key 值） |
| `/api_access/update_api_keys` | POST | 更新 API Key 配置 |
| `/api_access/del_api_keys` | GET/POST | 删除 API Key |

---

## 代码模式

### 推荐写法

**创建 API Key**

```java
import cn.dev33.satoken.apikey.SaApiKeyUtil;
import cn.dev33.satoken.apikey.model.ApiKeyModel;
import com.leatop.cdp.data.message.Message;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class ApiDemoController {

    @GetMapping("/apiKey")
    public Message<ApiKeyModel> createApiKey(@RequestParam String userId) {
        ApiKeyModel keyModel = SaApiKeyUtil.createApiKeyModel(userId);
        return Message.success(keyModel);
    }
}
```

**Scope 路径配置**

API Key 的 `scopes` 字段使用 Ant 风格路径列表（如 `/api/users/**`），通过 `AntPathMatcherUtils.match()` 匹配请求 URI。任一 scope 匹配即放行，全部不匹配则抛出 `ApiKeyScopeException`。

### 禁止事项

- **禁止在未激活 `apikey` profile 的情况下引入 apikey 依赖** -- 自动配置类不会加载，会导致认证链缺失
- **禁止直接操作 SA-Token 底层 Token 存储** -- 必须通过 `SaApiKeyUtil` 或内置 REST 接口管理 Key
- **禁止将 API Key 硬编码在业务代码中** -- 应通过 YAML 配置或数据库动态管理
- **禁止在运行时直接删除 YAML 中声明的 Key** -- 配置文件中的 Key 在启动时写入缓存，运行时删除需通过 `updateAllApiKeys()` 全量刷新
- **禁止在 scope 中依赖 HTTP 方法级别权限控制** -- Scope 仅支持路径匹配，不区分 GET/POST，如需区分需在业务层自行判断
- **禁止将 `expiresTime` 设为非法值** -- 使用 `-1` 表示永不过期，其他值必须为 13 位毫秒时间戳
- **禁止绕过 CDP 认证链自行实现 Key 校验逻辑** -- 应实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean 加入责任链
