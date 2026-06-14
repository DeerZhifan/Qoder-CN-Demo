# 知识库接口文档生成器

## 功能说明

本技能用于自动生成知识库后端接口的 Markdown 格式 API 文档。通过读取 Spring Controller 代码，按固定模板生成包含路径、方法、请求参数、响应结构、错误码和 curl 示例的完整接口文档。

## 使用方法

### 基本用法

当需要为某个 Controller 生成接口文档时，直接告诉 AI：

```
请为 KbDocumentController 生成接口文档
```

AI 会自动：
1. 读取 Controller 代码
2. 解析所有接口定义
3. 分析请求参数和响应结构
4. 生成完整的 Markdown 文档
5. 保存到 `docs/api/{ControllerName}.md`

### 批量生成

可以一次性要求生成多个 Controller 的文档：

```
请为所有 Controller 生成接口文档
```

### 更新文档

如果 Controller 代码有变更，可以要求重新生成：

```
请重新生成 KbCategoryController 的接口文档
```

## 输出位置

生成的文档保存在：
```
docs/api/
├── KbDocument.md      # 文档管理接口
├── KbCategory.md      # 分类管理接口
└── ...                # 其他 Controller 文档
```

## 文档结构

每个接口文档包含以下章节：

1. **基础信息**: Base Path、Content-Type 等
2. **接口列表**: 每个接口的详细定义
   - 接口描述
   - 请求信息（HTTP方法、路径、参数）
   - 请求字段说明表格
   - 响应示例（JSON）
   - 响应字段说明表格
   - 错误码列表
   - curl 示例
3. **附录**: 枚举值定义、通用错误码、注意事项

## 特性

### ✅ 自动解析

- HTTP 方法（GET/POST/PUT/DELETE）
- 路径参数（@PathVariable）
- Query 参数（@RequestParam）
- 请求体（@RequestBody + DTO）
- 响应结构（Result<T> 泛型解析）
- 参数校验注解（@NotNull、@NotBlank、@Size 等）

### ✅ 智能推断

- 从 JavaDoc 提取接口描述
- 从异常类型推断错误码
- 从 DTO 字段生成请求示例
- 从 Entity 字段生成响应示例

### ✅ 标准化输出

- 统一的文档模板
- 可执行的 curl 命令
- 清晰的字段说明表格
- 中文注释优先

## 示例

### 输入

```java
@RestController
@RequestMapping("/api/documents")
public class KbDocumentController {
    
    @GetMapping("/{id}")
    public Result<KbDocument> getDocument(@PathVariable Long id) {
        // ...
    }
}
```

### 输出

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

## 相关文件

- **SKILL.md**: 技能主文件，包含核心指令
- **reference.md**: 参考文档，提供详细示例和模式
- **docs/api/*.md**: 生成的接口文档

## 注意事项

1. **只读操作**: 技能不会修改 Controller 代码，只读取并生成文档
2. **保持一致性**: 所有生成的文档遵循相同的模板和格式
3. **真实信息**: 从代码和注解中提取真实信息，不臆造
4. **curl 可执行**: 生成的 curl 命令可以直接复制运行（可能需要调整 host/port）
5. **中文优先**: 接口描述、字段说明使用中文

## 技术栈

- **后端框架**: Spring Boot + MyBatis-Plus
- **响应包装**: Result<T> 统一返回
- **参数校验**: Jakarta Validation (@Valid, @NotNull 等)
- **分页支持**: MyBatis-Plus Page 对象

## 版本历史

- v1.0 (2026-06-11): 初始版本，支持基本的 Controller 解析和文档生成
