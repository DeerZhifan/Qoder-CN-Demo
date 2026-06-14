---
name: kb-api-doc-gen
description: 生成知识库后端接口文档。读取 Spring Controller 代码，按固定模板生成 Markdown 格式 API 文档并保存到 docs/api/ 目录。当用户要求生成接口文档、查看 API 定义或需要 curl 示例时使用。
---

# 知识库接口文档生成器

## 快速开始

当用户要求为某个 Controller 生成接口文档时：

1. 定位目标 Controller 文件（通常在 `backend/src/main/java/com/kb/manager/controller/`）
2. 解析所有 `@RequestMapping`、`@GetMapping`、`@PostMapping` 等注解
3. 提取请求参数、响应结构、错误码等信息
4. 按固定模板生成 Markdown 文档
5. 保存到 `docs/api/{ControllerName}.md`

## 文档模板

每个接口文档必须包含以下章节：

```markdown
# {Controller名称} 接口文档

## 基础信息

- **Base Path**: `{@RequestMapping 路径}`
- **Content-Type**: `application/json`

---

## 接口列表

### {接口名称}

**接口描述**: {从 JavaDoc 提取}

**请求**:
- **HTTP方法**: {GET/POST/PUT/DELETE}
- **路径**: `{完整路径}`
- **路径参数**: {如有}
- **Query参数**: {如有}
- **请求体**: {如有，展示 JSON 结构}

**请求字段说明**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| field1 | String | 是 | - | 字段说明 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| message | String | 响应消息 |
| data | Object | 业务数据 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误 |
| 404 | 资源不存在 |

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/xxx \
  -H "Content-Type: application/json"
```

---
```

## 工作流程

### 步骤 1: 解析 Controller

读取 Controller 类，提取：
- 类级别的 `@RequestMapping` 作为 Base Path
- 每个方法的 HTTP 方法（`@GetMapping` → GET）
- 路径拼接（Base Path + 方法级别路径）
- 参数类型和校验注解（`@Valid`、`@NotNull` 等）

### 步骤 2: 分析请求参数

**路径参数** (`@PathVariable`):
```java
@GetMapping("/{id}")
public Result<KbDocument> getDocument(@PathVariable Long id)
```
→ 路径参数: `id (Long)`

**Query 参数** (`@RequestParam`):
```java
@RequestParam(required = false) Long categoryId
@RequestParam(defaultValue = "1") Integer pageNum
```
→ Query 参数表格，标注必填/可选/默认值

**请求体** (`@RequestBody`):
- 读取对应的 DTO 类（如 `DocumentCreateRequest`）
- 提取字段名、类型、校验注解
- 生成 JSON 示例

### 步骤 3: 分析响应结构

**统一响应包装**:
所有接口返回 `Result<T>`，结构固定：
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1717747200000
}
```

**泛型 T 的解析**:
- `Result<Page<KbDocument>>` → data 是分页对象
- `Result<List<KbDocumentVersion>>` → data 是数组
- `Result<Long>` → data 是数字
- `Result<Void>` → data 是 null

**分页结构** (MyBatis-Plus Page):
```json
{
  "total": 25,
  "size": 10,
  "current": 1,
  "pages": 3,
  "records": []
}
```

### 步骤 4: 提取错误码

从以下来源提取：
1. Controller 方法注释中的异常说明
2. Service 层可能抛出的异常（`IllegalArgumentException` → 400）
3. 通用错误码（404、500 等）

常见映射：
- `IllegalArgumentException` → 400
- `IllegalStateException` → 400
- `NoSuchElementException` → 404
- 未捕获异常 → 500

### 步骤 5: 生成 curl 示例

根据 HTTP 方法和参数类型生成：

**GET 请求**:
```bash
curl -X GET "http://localhost:8080/api/documents?categoryId=1&status=PUBLISHED&pageNum=1&pageSize=10" \
  -H "Content-Type: application/json"
```

**POST 请求** (带请求体):
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 2,
    "title": "测试文档",
    "content": "# 测试内容"
  }'
```

**PUT 请求**:
```bash
curl -X PUT http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "更新后的标题",
    "content": "更新后的内容"
  }'
```

**DELETE 请求**:
```bash
curl -X DELETE http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```

## 关键规则

### 1. 路径拼接规则

```java
@RestController
@RequestMapping("/api/documents")  // Base Path
public class KbDocumentController {
    
    @GetMapping  // → GET /api/documents
    @GetMapping("/{id}")  // → GET /api/documents/{id}
    @PostMapping("/{id}/publish")  // → POST /api/documents/{id}/publish
}
```

### 2. 参数必填判断

- `@RequestParam(required = false)` → 可选
- `@RequestParam` (无 required) → 必填
- `@RequestBody @Valid` → 必填，且需校验
- `@PathVariable` → 必填

### 3. DTO 字段校验

从 DTO 类的注解提取：
```java
@NotNull(message = "分类ID不能为空")
private Long categoryId;  // → 必填

@NotBlank(message = "文档标题不能为空")
@Size(max = 200, message = "文档标题不能超过200个字符")
private String title;  // → 必填，最大200字符
```

### 4. 响应数据示例

**实体对象** (`KbDocument`):
```json
{
  "id": 101,
  "categoryId": 2,
  "title": "文档标题",
  "content": "文档内容",
  "status": "PUBLISHED",
  "version": 1,
  "publishTime": "2026-06-07T10:30:00",
  "createTime": "2026-06-07T09:00:00",
  "createBy": "张三",
  "updateTime": "2026-06-07T10:30:00",
  "updateBy": "张三"
}
```

**列表** (`List<KbDocumentVersion>`):
```json
[
  {
    "versionId": 301,
    "version": 3,
    "title": "版本标题",
    "content": "版本内容",
    "createTime": "2026-06-07T10:30:00",
    "createBy": "张三"
  }
]
```

### 5. 文档组织

- 一个 Controller 对应一个 Markdown 文件
- 文件名: `{ControllerName}.md` (去掉 Controller 后缀)
  - `KbDocumentController` → `KbDocument.md`
  - `KbCategoryController` → `KbCategory.md`
- 保存位置: `docs/api/`

## 输出示例

参考项目已有的 `docs/API_SPEC.md` 格式，但更简洁：
- 不需要数据库设计部分
- 不需要完整的流程示例
- 重点在单个接口的详细定义

## 注意事项

1. **不要修改 Controller 代码** - 只读操作
2. **保持模板一致性** - 所有接口文档格式统一
3. **提取真实信息** - 从代码和注解中提取，不要臆造
4. **curl 示例可执行** - 确保生成的 curl 命令可以直接运行
5. **中文注释优先** - 接口描述、字段说明使用中文

## 相关文件

- Controller 位置: `backend/src/main/java/com/kb/manager/controller/`
- DTO 位置: `backend/src/main/java/com/kb/manager/dto/`
- Entity 位置: `backend/src/main/java/com/kb/manager/entity/`
- 输出目录: `docs/api/`
- 参考文档: `docs/API_SPEC.md`
