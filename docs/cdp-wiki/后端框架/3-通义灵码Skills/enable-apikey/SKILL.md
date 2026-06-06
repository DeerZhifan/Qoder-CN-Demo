# 启用 CDP API Key 认证

## 描述

在已有 CDP 项目中启用 API Key 认证组件（`leatop-cdp-base-apikey`），支持 API Key 直传和 SaSign 签名两种认证方式。适用于第三方系统对接、开放接口等场景，支持按 API 路径配置访问范围（scope）。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **API Key 来源**（`yaml` 配置文件 或 `database` 数据库，默认 `yaml`）
3. **初始 Key 配置**（是否需要在 YAML 中预配置 API Key，默认是）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-apikey` 基于 SA-Token 的 API Key 机制，提供 API Key 和 SaSign 签名两种认证方式。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-apikey</artifactId>
</dependency>
```

## 步骤 2：激活 apikey profile

> API Key 组件通过 Spring profile 控制加载，必须激活 `apikey` profile 才会启用自动配置类 `CdpApiKeyAutoConfiguration`。

在 `application.yaml` 中添加 `apikey` 到 active profiles：

```yaml
spring:
  profiles:
    active: apikey
```

如果已有其他 profile，用逗号分隔：

```yaml
spring:
  profiles:
    active: dev,mysql,apikey
```

## 步骤 3：添加配置

> 以下为通过 YAML 配置文件定义 API Key 的方式。Key 在应用启动时由 `ApiManagerService` 预加载到 SA-Token 缓存中。如选择数据库方式，需创建 `frame_api_keys` 表。

在 `application-apikey.yaml` 中添加：

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

配置项说明：

| 属性 | 说明 |
|------|------|
| `title` | Key 名称/描述 |
| `intro` | 详细说明 |
| `apiKey` | Key 值（可自定义或由接口自动生成） |
| `loginId` | 关联的用户 ID |
| `expiresTime` | 过期时间，`-1` 永不过期，其他为 13 位毫秒时间戳 |
| `isValid` | 是否启用 |
| `scopes` | 允许访问的 API 路径列表（Ant 风格匹配） |

## 步骤 4：编写示例代码

> 以下示例展示如何通过代码创建 API Key，以及调用方如何携带 Key 访问接口。

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

**调用方携带 API Key**

```bash
# 方式1：请求参数
GET /api/users/list?apiKey=your-api-key-value

# 方式2：请求头
GET /api/users/list
Header: apiKey: your-api-key-value
```

**内置管理接口**（无需额外编码，组件自动注册）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api_access/get_api_keys` | GET | 查询所有 API Key |
| `/api_access/save_api_keys` | POST | 创建 API Key |
| `/api_access/update_api_keys` | POST | 更新 API Key |
| `/api_access/del_api_keys` | GET/POST | 删除 API Key |

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 `CdpApiKeyAutoConfiguration` 初始化信息
3. 使用 `apiKey` 请求头或参数访问受保护接口，确认返回正常数据
4. 不携带 Key 或携带无效 Key 访问受保护接口，确认返回认证失败
5. 携带有效 Key 访问 scope 范围外的接口，确认抛出 `ApiKeyScopeException`

---

## 完成后提醒

1. API Key 的 `scopes` 使用 Ant 风格路径匹配（如 `/api/**`），请求 URI 不在 scope 范围内会抛出 `ApiKeyScopeException`
2. 配置文件中声明的 Key 在启动时写入缓存，运行时删除需通过 `updateAllApiKeys()` 全量刷新
3. 数据库存储的 Key 优先级高于配置文件中的 Key
4. SaSign 签名方式需要调用方和服务端共享 secretKey，使用 SHA256 算法签名
5. 不要直接操作 SA-Token 底层 Token 存储，应通过 `SaApiKeyUtil` 或内置 REST 接口管理 Key
6. 如需扩展认证策略，实现 `AuthHandlerExecutor` 接口并注册为 Spring Bean 即可加入认证责任链
