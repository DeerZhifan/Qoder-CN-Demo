# 接口文档生成参考

## 完整示例：KbDocumentController

### Controller 代码片段

```java
@RestController
@RequestMapping("/api/documents")
@Validated
public class KbDocumentController {
    
    @Autowired
    private KbDocumentService documentService;
    
    /**
     * 分页查询文档列表
     * 
     * @param categoryId 分类ID(可选)
     * @param status 文档状态(可选)
     * @param title 文档标题(可选,支持模糊查询)
     * @param pageNum 页码,默认1
     * @param pageSize 每页数量,默认10
     * @return 分页结果
     */
    @GetMapping
    public Result<Page<KbDocument>> pageDocuments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // ...
    }
}
```

### 生成的接口文档

```markdown
# 知识库文档管理接口

## 基础信息

- **Base Path**: `/api/documents`
- **Content-Type**: `application/json`

---

## 接口列表

### 分页查询文档列表

**接口描述**: 根据条件分页查询文档列表，支持按分类、状态、标题筛选

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents`
- **Query参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| categoryId | Long | 否 | - | 分类ID，筛选指定分类下的文档 |
| status | String | 否 | - | 文档状态：DRAFT/PUBLISHED/OFFLINE |
| title | String | 否 | - | 文档标题，支持模糊查询 |
| pageNum | Integer | 否 | 1 | 页码，从1开始 |
| pageSize | Integer | 否 | 10 | 每页数量，范围1-100 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "size": 10,
    "current": 1,
    "pages": 3,
    "records": [
      {
        "id": 101,
        "categoryId": 2,
        "title": "Vue3组件化开发最佳实践",
        "content": "# Vue3组件化开发...",
        "status": "PUBLISHED",
        "version": 3,
        "publishTime": "2026-06-05T10:30:00",
        "createTime": "2026-05-20T09:00:00",
        "createBy": "张三",
        "updateTime": "2026-06-05T10:30:00",
        "updateBy": "张三"
      }
    ]
  },
  "timestamp": 1717747200000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 分页数据 |
| data.total | Long | 总记录数 |
| data.size | Integer | 每页数量 |
| data.current | Integer | 当前页码 |
| data.pages | Integer | 总页数 |
| data.records | Array | 文档列表 |
| data.records[].id | Long | 文档ID |
| data.records[].categoryId | Long | 分类ID |
| data.records[].title | String | 文档标题 |
| data.records[].content | String | 文档内容（Markdown格式） |
| data.records[].status | String | 文档状态：DRAFT/PUBLISHED/OFFLINE |
| data.records[].version | Integer | 版本号 |
| data.records[].publishTime | String/null | 发布时间，ISO 8601格式 |
| data.records[].createTime | String | 创建时间，ISO 8601格式 |
| data.records[].createBy | String | 创建人 |
| data.records[].updateTime | String | 更新时间，ISO 8601格式 |
| data.records[].updateBy | String | 更新人 |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如pageSize超出范围） |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X GET "http://localhost:8080/api/documents?categoryId=2&status=PUBLISHED&pageNum=1&pageSize=10" \
  -H "Content-Type: application/json"
```

---
```

## DTO 解析示例

### DocumentCreateRequest.java

```java
@Data
public class DocumentCreateRequest {
    
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题不能超过200个字符")
    private String title;
    
    private String content;
}
```

### 生成的请求体文档

```markdown
**请求体**:

```json
{
  "categoryId": 2,
  "title": "TypeScript高级类型技巧",
  "content": "# TypeScript高级类型\n\n## 泛型约束..."
}
```

**请求字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 是 | 分类ID |
| title | String | 是 | 文档标题，最大200字符 |
| content | String | 否 | 文档内容（Markdown格式） |
```

## Entity 解析示例

### KbDocument.java

```java
@Data
@TableName("kb_document")
public class KbDocument implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("category_id")
    private Long categoryId;
    
    @TableField("title")
    private String title;
    
    @TableField("content")
    private String content;
    
    @TableField("status")
    private String status;
    
    @TableField("version")
    private Integer version;
    
    @TableField("publish_time")
    private LocalDateTime publishTime;
    
    @TableField("create_by")
    private String createBy;
    
    @TableField("update_by")
    private String updateBy;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
```

### 生成的响应字段说明

```markdown
**KbDocument 字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID（雪花算法生成） |
| categoryId | Long | 分类ID |
| title | String | 文档标题 |
| content | String | 文档内容（Markdown格式） |
| status | String | 文档状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线 |
| version | Integer | 当前版本号 |
| publishTime | LocalDateTime | 发布时间 |
| createBy | String | 创建人 |
| updateBy | String | 更新人 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除标识：0-未删除，1-已删除（不返回给前端） |
```

## 常见模式

### 模式 1: 简单 GET 请求

```java
@GetMapping("/{id}")
public Result<KbDocument> getDocument(@PathVariable Long id)
```

→ 

```markdown
### 获取文档详情

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}`
- **路径参数**: id (Long) - 文档ID

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```
```

### 模式 2: POST 带请求体

```java
@PostMapping
public Result<Long> createDocument(@RequestBody @Valid DocumentCreateRequest request)
```

→

```markdown
### 创建文档草稿

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents`
- **请求体**: (见 DocumentCreateRequest 结构)

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 2,
    "title": "测试文档",
    "content": "# 测试内容"
  }'
```
```

### 模式 3: 复合路径 POST

```java
@PostMapping("/{id}/publish")
public Result<Void> publishDocument(@PathVariable Long id)
```

→

```markdown
### 发布文档

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/publish`
- **路径参数**: id (Long) - 文档ID

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents/101/publish \
  -H "Content-Type: application/json"
```
```

### 模式 4: DELETE 请求

```java
@DeleteMapping("/{id}")
public Result<Void> deleteDocument(@PathVariable Long id)
```

→

```markdown
### 删除文档

**请求**:
- **HTTP方法**: DELETE
- **路径**: `/api/documents/{id}`
- **路径参数**: id (Long) - 文档ID

**curl 示例**:

```bash
curl -X DELETE http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```
```

## 特殊场景处理

### 场景 1: 分页响应

当返回类型为 `Result<Page<T>>` 时，data 结构为 MyBatis-Plus 的分页对象：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10,
    "records": []
  }
}
```

### 场景 2: 列表响应

当返回类型为 `Result<List<T>>` 时，data 直接是数组：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "分类1"
    }
  ]
}
```

### 场景 3: Void 响应

当返回类型为 `Result<Void>` 时，data 为 null：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 场景 4: 简单类型响应

当返回类型为 `Result<Long>` 或 `Result<String>` 时：

```json
{
  "code": 200,
  "message": "success",
  "data": 123
}
```

## 错误码提取规则

### 从 Service 层异常推断

```java
// Service 代码
public void publishDocument(Long id) {
    KbDocument doc = getById(id);
    if (doc == null) {
        throw new NoSuchElementException("文档不存在");  // → 404
    }
    if (!"DRAFT".equals(doc.getStatus())) {
        throw new IllegalStateException("只有草稿状态的文档可以发布");  // → 400
    }
}
```

→

```markdown
**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 只有草稿状态的文档可以发布 |
| 404 | 文档不存在 |
```

### 通用错误码

所有接口都应包含：

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证（如果项目有认证） |
| 403 | 无权限（如果项目有权限控制） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 注意事项

1. **不要包含敏感信息** - curl 示例中不包含真实 token
2. **保持示例简洁** - 响应示例只展示关键字段，不需要展示所有字段
3. **中文优先** - 接口描述、字段说明使用中文
4. **一致性** - 同一 Controller 的所有接口格式保持一致
5. **可执行性** - curl 命令应该可以直接复制运行（可能需要调整 host/port）
