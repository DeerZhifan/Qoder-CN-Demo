---
trigger: when_referenced
knowledge_source:
  - cdp-coding-standards
  - cdp-module-common-util
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 编写或修改 Controller 类的 JavaDoc 注释
- 编写或修改 DTO、QO 类的字段注释
- 配置 `smart-doc.json` 文件
- 使用 `smart-doc-maven-plugin` 生成 API 文档
- 执行 `mvn smart-doc:html` 命令

---

## 前置依赖

1. Maven 插件（在根 `pom.xml` 的 `<pluginManagement>` 中已统一管理版本）：

```xml
<plugin>
    <groupId>com.ly.smart-doc</groupId>
    <artifactId>smart-doc-maven-plugin</artifactId>
    <configuration>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

2. 在 `src/main/resources/` 下创建 `smart-doc.json` 配置文件。

3. 版本号由根 POM 属性 `maven-smart-doc.version`（当前 `3.0.3`）管理，子模块不需要指定版本号。

---

## 配置要点

### smart-doc.json 配置模板

```json
{
  "serverUrl": "/",
  "isStrict": false,
  "allInOne": true,
  "outPath": "src/main/resources/static/api-docs",
  "createDebugPage": false,
  "showValidation": false,
  "showAuthor": false,
  "allInOneDocFileName": "index.html",
  "projectName": "项目名称-API接口文档",
  "requestHeaders": [
    {
      "name": "cdp-token",
      "type": "string",
      "desc": "接口认证token",
      "value": "",
      "required": true,
      "since": "-"
    }
  ]
}
```

### 文档生成命令

```bash
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html
```

生成的文档输出到 `outPath` 指定目录，默认为 `src/main/resources/static/api-docs`。

### 分组配置

通过 `groups` 按业务模块组织 API：

```json
{
  "groups": [
    {
      "name": "业务组件-系统管理",
      "apis": "com.leatop.cdp.system.controller"
    }
  ]
}
```

通过 `packageExcludeFilters` 排除不需要生成文档的 Controller 包。

---

## 代码模式

### 推荐写法

**Controller 类注释（Smart-doc 依赖此生成文档标题）：**

```java
/**
 * API Key 控制器
 *
 * @author luowei
 * @date 2026/1/20 15:26
 */
@RestController
@RequestMapping("/api_access")
public class ApiKeyController { }
```

**Controller 方法注释（`@param` 和 `@return` 必填）：**

```java
/**
 * 获取所有 API Key 列表
 *
 * @return API Key 列表
 */
@GetMapping("/get_api_keys")
public Message<List<ApiKeyDTO>> getApiKeys() {
    return apiKeyService.getApiKeys();
}

/**
 * 分页查询数据源列信息
 *
 * @param qo 数据源连接信息（含分页参数）
 * @return 分页列信息
 */
@PostMapping("/getColumnPage")
public Message<Page<TableColumnInfoDto>> getColumnPage(
        @RequestBody DataSourceConnectionInfoQo qo) {
    return dataSourceService.getColumnPage(qo);
}
```

**DTO/QO 字段注释（字段 Javadoc 会作为参数描述出现在文档中）：**

```java
public class NewsItemDto {

    /** 主键 */
    private String id;

    /** 文章标题 */
    private String title;

    /** 状态：0-未解析，1-解析成功，2-解析异常 */
    private Integer status;
}
```

**注释要求速查表：**

| 位置 | 要求 |
|------|------|
| Controller 类 | 必须有类注释（含 `@author`、`@date`） |
| Controller 方法 | 必须有方法注释（含 `@param`、`@return`） |
| DTO/QO 字段 | 必须有字段注释（`/** 业务含义 */`） |
| 枚举值 | 必须有字段注释说明含义 |

**`@date` 格式统一为** `yyyy/M/d HH:mm`，例如 `2026/1/20 15:26`。

### 禁止事项

- **禁止使用 Swagger 注解** -- 项目使用 Smart-doc 生成文档，禁止引入 `@Api`、`@ApiOperation`、`@ApiModel`、`@ApiModelProperty` 等 Swagger 注解
- **禁止 Controller 公开方法缺少 JavaDoc** -- 所有公开方法必须有完整的 JavaDoc 注释（含 `@param` 和 `@return`），否则生成的文档缺少描述信息
- **禁止 DTO/QO 字段缺少注释** -- 每个字段必须有 `/** 业务含义 */` 格式的 Javadoc 注释
- **禁止 `@param` 参数名与方法签名不一致** -- 注释中的参数名必须与方法形参名完全匹配，否则 Smart-doc 无法正确关联
- **禁止使用行内注释替代 Javadoc** -- Smart-doc 只读取 `/** */` 格式的 Javadoc，`//` 和 `/* */` 注释不会被识别
- **禁止在子模块中硬编码 smart-doc 插件版本号** -- 版本由根 POM 的 `maven-smart-doc.version` 属性统一管理
