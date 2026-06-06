# 生成 Smart-doc API 文档

## 描述

为 CDP 项目模块配置 Smart-doc 插件并生成 HTML 格式的 API 接口文档。Smart-doc 从 Controller 方法的 JavaDoc 注释自动提取接口信息，不依赖 Swagger 等运行时注解。

## 输入

请向用户确认以下信息：

1. **模块路径**（目标模块的相对路径，如 `leatop-cdp-example/leatop-cdp-example-demo1`）
2. **项目名称**（文档标题，如 `CDP框架API接口文档`）
3. **是否需要分组**（按 Controller 包名对 API 分组，默认否）

---

## 步骤 1：添加 smart-doc-maven-plugin

> Smart-doc 插件版本由根 POM 的 `maven-smart-doc.version` 属性统一管理（当前 `3.0.3`），子模块中不需要指定版本号。

在目标模块的 `pom.xml` 的 `<build><plugins>` 中添加：

```xml
<!-- smart-doc生成文档 -->
<plugin>
    <groupId>com.ly.smart-doc</groupId>
    <artifactId>smart-doc-maven-plugin</artifactId>
    <configuration>
    </configuration>
    <executions>
        <execution>
            <!--不需要在编译项目时自动生成文档可注释phase-->
<!--            <phase>compile</phase>-->
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 步骤 2：创建 smart-doc.json 配置文件

> 配置文件路径固定为 `src/main/resources/smart-doc.json`，根 POM 通过 `<configFile>${basedir}/src/main/resources/smart-doc.json</configFile>` 指定。

在目标模块的 `src/main/resources/` 下创建 `smart-doc.json`：

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
  "projectName": "{项目名称}",
  "requestHeaders": [
    {
      "name": "cdp-token",
      "type": "string",
      "desc": "接口认证token",
      "value": "",
      "required": true,
      "since": "-"
    },
    {
      "name": "Accept-Sign-Ignore",
      "type": "string",
      "desc": "忽略接口参数验签",
      "value": "BIGBIGGIRL",
      "required": false,
      "since": "-"
    }
  ]
}
```

如用户需要分组，添加 `groups` 和 `packageExcludeFilters` 字段：

```json
{
  "packageExcludeFilters": "com.leatop.example.controller",
  "groups": [
    {
      "name": "业务组件-系统管理",
      "apis": "com.leatop.cdp.system.controller"
    },
    {
      "name": "业务组件-附件管理",
      "apis": "com.leatop.cdp.file.controller"
    }
  ]
}
```

## 步骤 3：检查 JavaDoc 注释完整性

> Smart-doc 从 JavaDoc 注释提取文档内容，注释缺失会导致生成的文档缺少描述信息。

逐项检查目标模块中的 Controller 类：

1. **Controller 类**必须有类级 JavaDoc（含 `@author`、`@date`）：

```java
/**
 * 新闻条目控制器
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@RestController
@RequestMapping("/news_research/news_item")
public class NewsItemController { }
```

2. **Controller 方法**必须有方法级 JavaDoc（含 `@param`、`@return`）：

```java
/**
 * 分页查询
 *
 * @param qo 查询参数
 * @return 分页列表
 */
@PostMapping("/listPage")
public Message<Page<NewsItemDto>> listPage(@Validated NewsItemPageQo qo) {
    return newsItemService.listPage(qo);
}
```

3. **DTO/QO 字段**必须有字段注释：

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

如有缺失注释，补全后再执行生成。

## 步骤 4：执行文档生成

在目标模块目录下执行：

```bash
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html
```

或在项目根目录指定模块执行：

```bash
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html -f {模块路径}
```

## 步骤 5：验证生成结果

检查以下内容：

1. 命令执行无报错，控制台输出文档生成成功信息
2. `src/main/resources/static/api-docs/index.html` 文件已生成
3. 用浏览器打开 `index.html`，确认：
   - 接口列表完整，无遗漏的 Controller
   - 每个接口有描述信息（来自方法 JavaDoc）
   - 请求参数和响应字段有说明（来自 DTO/QO 字段注释）
   - 分组显示正确（如配置了 groups）

---

## 完成后提醒

1. 项目使用 Smart-doc 生成文档，**禁止使用** Swagger 的 `@Api`、`@ApiOperation` 等注解
2. 所有 Controller 公开方法和 DTO/QO 字段必须有 JavaDoc 注释，否则文档中缺少描述
3. `@param` 参数名必须与方法形参名一致，否则 Smart-doc 无法正确关联
4. Smart-doc 只识别 `/** */` 格式的 Javadoc 注释，`//` 和 `/* */` 不会被提取
5. 如需在编译阶段自动生成文档，取消 `pom.xml` 中 `<phase>compile</phase>` 的注释
6. 详细注释规范参见 `doc/comment-spec.md`
