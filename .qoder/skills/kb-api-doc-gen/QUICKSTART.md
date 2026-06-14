# 知识库接口文档生成器 - 快速开始

## 📖 这是什么？

这是一个 Qoder 技能，可以自动从 Spring Boot Controller 代码生成专业的 API 接口文档。

## 🚀 快速使用

### 场景 1: 为单个 Controller 生成文档

**你说**:
```
请为 KbDocumentController 生成接口文档
```

**AI 会**:
1. 读取 `backend/src/main/java/com/kb/manager/controller/KbDocumentController.java`
2. 解析所有接口（GET、POST、PUT、DELETE）
3. 提取请求参数、响应结构、错误码
4. 生成 Markdown 文档
5. 保存到 `docs/api/KbDocument.md`

### 场景 2: 批量生成所有 Controller 文档

**你说**:
```
请为所有 Controller 生成接口文档
```

**AI 会**:
- 遍历所有 Controller 文件
- 为每个 Controller 生成独立的文档
- 保存到 `docs/api/` 目录

### 场景 3: 更新已有文档

**你说**:
```
KbCategoryController 有更新，请重新生成接口文档
```

**AI 会**:
- 重新读取最新的 Controller 代码
- 覆盖原有的 `docs/api/KbCategory.md`

## 📋 生成的文档包含什么？

每个接口文档都包含：

✅ **基础信息**
- Base Path（如 `/api/documents`）
- Content-Type

✅ **每个接口的详细信息**
- 接口描述（从 JavaDoc 提取）
- HTTP 方法和路径
- 路径参数、Query 参数、请求体
- 请求字段说明表格（含必填、类型、默认值）
- 响应示例（真实 JSON 格式）
- 响应字段说明表格
- 错误码列表
- curl 示例（可直接运行）

✅ **附录**
- 枚举值定义
- 通用错误码
- 注意事项

## 💡 实际示例

### 输入代码

```java
@RestController
@RequestMapping("/api/documents")
public class KbDocumentController {
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public Result<KbDocument> getDocument(@PathVariable Long id) {
        KbDocument doc = documentService.getDocumentById(id);
        return Result.success(doc);
    }
}
```

### 输出文档

```markdown
### 获取文档详情

**接口描述**: 获取指定文档的详细信息，包括完整内容

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}`
- **路径参数**: id (Long) - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
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
  },
  "timestamp": 1717747200000
}
```

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```
```

## 🎯 核心特性

### 1. 智能解析

- ✅ 自动识别 HTTP 方法
- ✅ 提取路径参数和 Query 参数
- ✅ 解析 DTO 字段和校验规则
- ✅ 推断响应结构（支持泛型）
- ✅ 从异常类型推断错误码

### 2. 标准化输出

- ✅ 统一的文档模板
- ✅ 清晰的表格格式
- ✅ 可执行的 curl 命令
- ✅ 中文注释优先

### 3. 安全可靠

- ✅ 只读操作，不修改代码
- ✅ 从真实代码提取，不臆造
- ✅ 保持与代码同步

## 📂 文件结构

```
.qoder/skills/kb-api-doc-gen/
├── SKILL.md           # 技能主文件（核心指令）
├── reference.md       # 参考文档（详细示例）
└── README.md          # 使用说明

docs/api/
├── KbDocument.md      # 文档管理接口（已生成）
├── KbCategory.md      # 分类管理接口（已生成）
└── ...                # 其他 Controller 文档
```

## 🔧 技术细节

### 支持的注解

| 注解 | 用途 | 示例 |
|------|------|------|
| @GetMapping | GET 请求 | `@GetMapping("/{id}")` |
| @PostMapping | POST 请求 | `@PostMapping` |
| @PutMapping | PUT 请求 | `@PutMapping("/{id}")` |
| @DeleteMapping | DELETE 请求 | `@DeleteMapping("/{id}")` |
| @PathVariable | 路径参数 | `@PathVariable Long id` |
| @RequestParam | Query 参数 | `@RequestParam String title` |
| @RequestBody | 请求体 | `@RequestBody DocumentCreateRequest request` |
| @Valid | 参数校验 | `@RequestBody @Valid DocumentCreateRequest` |

### 支持的响应类型

| 返回类型 | 说明 | 示例 |
|----------|------|------|
| `Result<T>` | 单个对象 | `Result<KbDocument>` |
| `Result<List<T>>` | 列表 | `Result<List<KbDocumentVersion>>` |
| `Result<Page<T>>` | 分页 | `Result<Page<KbDocument>>` |
| `Result<Long>` | 简单类型 | `Result<Long>` (返回ID) |
| `Result<Void>` | 无数据 | `Result<Void>` (data=null) |

### 错误码映射

| 异常类型 | HTTP 状态码 | 说明 |
|----------|-------------|------|
| IllegalArgumentException | 400 | 参数非法 |
| IllegalStateException | 400 | 状态非法 |
| NoSuchElementException | 404 | 资源不存在 |
| 未捕获异常 | 500 | 服务器错误 |

## ❓ 常见问题

### Q: 如何确保文档与代码同步？

A: 每次 Controller 代码变更后，重新运行技能生成文档即可。

### Q: 可以自定义文档模板吗？

A: 可以修改 `SKILL.md` 中的"文档模板"部分，调整输出格式。

### Q: 支持其他项目吗？

A: 只要项目使用 Spring Boot + Result<T> 统一返回，都可以使用此技能。可能需要根据具体项目调整模板。

### Q: curl 示例中的端口是多少？

A: 默认使用 `localhost:8080`，你可以根据实际项目配置修改 `SKILL.md` 中的模板。

## 📝 最佳实践

1. **及时更新**: Controller 代码变更后，立即重新生成文档
2. **检查生成结果**: AI 生成后，快速浏览确认关键信息正确
3. **补充业务说明**: 对于复杂的业务逻辑，可以在文档中手动补充说明
4. **保持一致性**: 所有 Controller 使用相同的文档风格

## 🎉 开始使用

现在你可以尝试：

```
请为 KbDocumentController 生成接口文档
```

或者查看已生成的示例：
- [docs/api/KbDocument.md](../../docs/api/KbDocument.md)
- [docs/api/KbCategory.md](../../docs/api/KbCategory.md)
