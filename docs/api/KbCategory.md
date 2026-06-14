# 知识库分类管理接口文档

## 基础信息

- **Base Path**: `/api/categories`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 接口列表

### 1. 获取分类树形结构

**接口描述**: 获取所有分类的树形结构，用于前端展示分类层级关系

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/categories/tree`

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "parentId": null,
      "name": "技术文档",
      "sortOrder": 1,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "name": "前端开发",
          "sortOrder": 1,
          "children": []
        },
        {
          "id": 3,
          "parentId": 1,
          "name": "后端开发",
          "sortOrder": 2,
          "children": []
        }
      ]
    },
    {
      "id": 4,
      "parentId": null,
      "name": "产品文档",
      "sortOrder": 2,
      "children": []
    }
  ],
  "timestamp": 1717747200000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200表示成功 |
| message | String | 响应消息 |
| data | Array | 分类树列表（根节点） |
| data[].id | Long | 分类ID |
| data[].parentId | Long/null | 父分类ID，根节点为null |
| data[].name | String | 分类名称 |
| data[].sortOrder | Integer | 排序序号，数值越小越靠前 |
| data[].children | Array | 子分类列表，递归结构 |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/categories/tree \
  -H "Content-Type: application/json"
```

---

### 2. 新增分类

**接口描述**: 创建新的分类节点

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/categories`
- **请求体**:

```json
{
  "parentId": 1,
  "name": "移动开发",
  "sortOrder": 3
}
```

**请求字段说明**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| parentId | Long | 是 | - | 父分类ID，null表示根节点 |
| name | String | 是 | - | 分类名称，最大100字符 |
| sortOrder | Integer | 否 | 0 | 排序序号 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": 5,
  "timestamp": 1717747200000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| message | String | 响应消息 |
| data | Long | 新创建的分类ID |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如分类名称为空、父分类不存在） |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": 1,
    "name": "移动开发",
    "sortOrder": 3
  }'
```

---

### 3. 修改分类

**接口描述**: 更新指定分类的信息（仅可修改名称和排序）

**请求**:
- **HTTP方法**: PUT
- **路径**: `/api/categories/{id}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分类ID |

**请求体**:

```json
{
  "name": "前端技术",
  "sortOrder": 2
}
```

**请求字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分类名称，最大100字符 |
| sortOrder | Integer | 否 | 排序序号 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1717747200000
}
```

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如分类名称为空） |
| 404 | 分类不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X PUT http://localhost:8080/api/categories/2 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "前端技术",
    "sortOrder": 2
  }'
```

---

### 4. 删除分类

**接口描述**: 软删除指定分类（标记为已删除，不物理删除）

**请求**:
- **HTTP方法**: DELETE
- **路径**: `/api/categories/{id}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分类ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1717747200000
}
```

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 该分类下存在子分类或文档，无法删除 |
| 404 | 分类不存在 |
| 500 | 服务器内部错误 |

**注意事项**:
- 如果分类下存在子分类或关联文档，则不允许删除
- 采用软删除机制，数据库中保留记录但标记为deleted

**curl 示例**:

```bash
curl -X DELETE http://localhost:8080/api/categories/5 \
  -H "Content-Type: application/json"
```

---

### 5. 按分类统计文档数量

**接口描述**: 获取每个分类下的文档数量统计

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/categories/document-count`

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "categoryId": 1,
      "categoryName": "技术文档",
      "documentCount": 25
    },
    {
      "categoryId": 2,
      "categoryName": "前端开发",
      "documentCount": 15
    },
    {
      "categoryId": 3,
      "categoryName": "后端开发",
      "documentCount": 10
    }
  ],
  "timestamp": 1717747200000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| message | String | 响应消息 |
| data | Array | 分类文档数量统计列表 |
| data[].categoryId | Long | 分类ID |
| data[].categoryName | String | 分类名称 |
| data[].documentCount | Long | 该分类下的文档数量 |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/categories/document-count \
  -H "Content-Type: application/json"
```

---

## 附录

### 通用错误码

所有接口都可能返回以下错误码：

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证（如果项目有认证） |
| 403 | 无权限（如果项目有权限控制） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 注意事项

1. **认证授权**: 所有接口需要在请求头中携带认证token（如果项目启用了认证）
   ```
   Authorization: Bearer <token>
   ```

2. **树形结构**: 
   - 分类支持多级嵌套，通过parentId建立父子关系
   - parentId为null表示根节点
   - children字段递归包含子分类

3. **排序规则**: 
   - sortOrder数值越小，排序越靠前
   - 同一父分类下的子分类按sortOrder排序

4. **删除限制**: 
   - 分类下存在子分类或文档时不允许删除
   - 需要先删除子分类和文档，才能删除父分类

5. **软删除**: 删除操作采用软删除机制，数据库中保留记录但标记为deleted

---

**文档版本**: v1.0  
**最后更新**: 2026-06-11  
**生成工具**: kb-api-doc-gen Skill
