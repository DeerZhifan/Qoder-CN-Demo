# 如何使用 CDP API Key 认证组件

## 概述

API Key 认证组件（`leatop-cdp-base-apikey`）基于 SA-Token 的 API Key 机制，为系统提供 API Key 和 SaSign 签名两种接口认证方式。适用于第三方系统对接、开放接口等场景，支持按 API 路径配置访问范围（scope）。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-apikey</artifactId>
</dependency>
```

**2. 激活 apikey profile：**

```yaml
spring:
  profiles:
    active: apikey
```

**3. 配置数据库表（可选，支持数据库存储 API Key）：**

- `frame_api_keys` — API Key 存储表

## 配置项

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

## 核心认证方式

### 方式一：API Key 认证

请求时在请求头或参数中携带 API Key，框架自动校验 Key 的有效性和 scope 范围。

认证流程：
1. 从请求中读取 API Key
2. 校验 Key 是否有效、是否过期
3. 使用 `AntPathMatcher` 匹配请求 URI 是否在 Key 的 `scopes` 范围内
4. 校验通过后将用户信息写入上下文

### 方式二：SaSign 签名认证

使用 SHA256 签名方式，请求头携带 `Authorization: SaSign ...`，包含 appid、timestamp、nonce、sign 参数。

## 内置 REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api_access/get_api_keys` | GET | 查询所有 API Key |
| `/api_access/save_api_keys` | POST | 创建 API Key（自动生成 key 值） |
| `/api_access/update_api_keys` | POST | 更新 API Key 配置 |
| `/api_access/del_api_keys` | GET/POST | 删除 API Key |

## 使用示例

### 创建 API Key

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

### 调用方携带 API Key

```bash
# 方式1：请求参数
GET /api/users/list?apiKey=your-api-key-value

# 方式2：请求头
GET /api/users/list
Header: apiKey: your-api-key-value
```

## 注意事项

> 注意：API Key 的 `scopes` 使用 Ant 风格路径匹配（如 `/api/**`），请求 URI 不在 scope 范围内会抛出 `ApiKeyScopeException`。

> 注意：API Key 支持两种存储方式：配置文件（`api-manage.api-keys`）和数据库（`frame_api_keys` 表），数据库优先。

> 注意：`expiresTime` 设为 `-1` 表示永不过期，其他值为 13 位毫秒时间戳。

> 注意：SaSign 签名方式需要调用方和服务端共享 secretKey，使用 SHA256 算法签名。
