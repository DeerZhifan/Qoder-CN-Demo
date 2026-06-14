# 知识库文档管理接口文档

## 基础信息

- **Base Path**: `/api/documents`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 接口列表

### 1. 分页查询文档列表

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
        "updateBy": "张三",
        "deleted": 0
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
| data | Object | 分页数据（MyBatis-Plus Page对象） |
| data.total | Long | 总记录数 |
| data.size | Integer | 每页数量 |
| data.current | Integer | 当前页码 |
| data.pages | Integer | 总页数 |
| data.records | Array | 文档列表 |
| data.records[].id | Long | 文档ID（雪花算法生成） |
| data.records[].categoryId | Long | 分类ID |
| data.records[].title | String | 文档标题 |
| data.records[].content | String | 文档内容（Markdown格式） |
| data.records[].status | String | 文档状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线 |
| data.records[].version | Integer | 当前版本号 |
| data.records[].publishTime | String/null | 发布时间，ISO 8601格式 |
| data.records[].createTime | String | 创建时间，ISO 8601格式 |
| data.records[].createBy | String | 创建人 |
| data.records[].updateTime | String | 更新时间，ISO 8601格式 |
| data.records[].updateBy | String | 更新人 |
| data.records[].deleted | Integer | 逻辑删除标识：0-未删除，1-已删除 |
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

### 2. 获取文档详情

**接口描述**: 获取指定文档的详细信息，包括完整内容

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 101,
    "categoryId": 2,
    "title": "Vue3组件化开发最佳实践",
    "content": "# Vue3组件化开发\n\n## 概述\n\nVue3引入了Composition API...",
    "status": "PUBLISHED",
    "version": 3,
    "publishTime": "2026-06-05T10:30:00",
    "createTime": "2026-05-20T09:00:00",
    "createBy": "张三",
    "updateTime": "2026-06-05T10:30:00",
    "updateBy": "张三",
    "deleted": 0
  },
  "timestamp": 1717747200000
}
```

**响应字段说明**: 同"分页查询文档列表"中的 records[] 字段

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```

---

### 3. 创建文档草稿

**接口描述**: 创建新的文档草稿，初始状态为DRAFT

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents`
- **请求体**:

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

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": 103,
  "timestamp": 1717747200000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| message | String | 响应消息 |
| data | Long | 新创建的文档ID |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如标题为空、分类ID为空） |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 2,
    "title": "TypeScript高级类型技巧",
    "content": "# TypeScript高级类型\n\n## 泛型约束..."
  }'
```

---

### 4. 更新文档

**接口描述**: 更新文档信息，仅草稿状态的文档可以更新

**请求**:
- **HTTP方法**: PUT
- **路径**: `/api/documents/{id}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

**请求体**:

```json
{
  "title": "TypeScript高级类型技巧(修订版)",
  "content": "# TypeScript高级类型\n\n## 更新后的内容..."
}
```

**请求字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文档标题，最大200字符 |
| content | String | 否 | 文档内容（Markdown格式） |

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
| 400 | 只有草稿状态的文档可以更新 / 参数错误 |
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X PUT http://localhost:8080/api/documents/103 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "TypeScript高级类型技巧(修订版)",
    "content": "# TypeScript高级类型\n\n## 更新后的内容..."
  }'
```

---

### 5. 发布文档

**接口描述**: 发布文档，将草稿状态变更为已发布，并创建新版本

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/publish`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

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
| 400 | 只有草稿状态的文档可以发布 |
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents/103/publish \
  -H "Content-Type: application/json"
```

---

### 6. 下线文档

**接口描述**: 下线已发布的文档，状态变更为OFFLINE

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/offline`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

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
| 400 | 只有已发布的文档可以下线 |
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents/101/offline \
  -H "Content-Type: application/json"
```

---

### 7. 删除文档

**接口描述**: 软删除指定文档（标记为已删除，不物理删除）

**请求**:
- **HTTP方法**: DELETE
- **路径**: `/api/documents/{id}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

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
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X DELETE http://localhost:8080/api/documents/101 \
  -H "Content-Type: application/json"
```

---

### 8. 获取版本列表

**接口描述**: 获取指定文档的所有历史版本列表

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}/versions`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 301,
      "documentId": 101,
      "version": 3,
      "title": "Vue3组件化开发最佳实践",
      "content": "# Vue3组件化开发\n\n## 最新版本内容...",
      "createTime": "2026-06-05T10:30:00",
      "createBy": "张三"
    },
    {
      "id": 302,
      "documentId": 101,
      "version": 2,
      "title": "Vue3组件化开发",
      "content": "# Vue3组件化开发\n\n## 第二版内容...",
      "createTime": "2026-05-28T15:20:00",
      "createBy": "张三"
    },
    {
      "id": 303,
      "documentId": 101,
      "version": 1,
      "title": "Vue3入门",
      "content": "# Vue3入门\n\n## 初始版本内容...",
      "createTime": "2026-05-20T09:00:00",
      "createBy": "张三"
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
| data | Array | 版本列表 |
| data[].id | Long | 版本ID（雪花算法生成） |
| data[].documentId | Long | 文档ID |
| data[].version | Integer | 版本号，从1开始递增 |
| data[].title | String | 该版本的标题 |
| data[].content | String | 该版本的内容（Markdown格式） |
| data[].createTime | String | 版本创建时间，ISO 8601格式 |
| data[].createBy | String | 版本创建人 |
| timestamp | Long | 响应时间戳 |

**错误码**:

| 错误码 | 说明 |
|--------|------|
| 404 | 文档不存在 |
| 500 | 服务器内部错误 |

**curl 示例**:

```bash
curl -X GET http://localhost:8080/api/documents/101/versions \
  -H "Content-Type: application/json"
```

---

### 9. 版本回滚

**接口描述**: 将文档回滚到指定的历史版本，创建新的草稿

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/rollback/{versionId}`
- **路径参数**: 

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |
| versionId | Long | 是 | 要回滚到的版本ID |

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
| 404 | 文档或版本不存在 |
| 500 | 服务器内部错误 |

**注意事项**:
- 回滚操作会基于指定版本创建一个新的草稿
- 新版本号在当前最大版本号基础上+1
- 回滚后的文档状态为DRAFT，需要重新发布才能生效
- 原已发布的版本保持不变，作为历史记录保留

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/documents/101/rollback/302 \
  -H "Content-Type: application/json"
```

---

## 附录

### 枚举值定义

#### DocumentStatus (文档状态)

| 枚举值 | 说明 |
|--------|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| OFFLINE | 已下线 |

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

2. **幂等性**: 
   - GET、DELETE接口具有幂等性
   - POST、PUT接口不保证幂等性，客户端需做好防重复提交

3. **分页限制**: 
   - pageSize最大值为100，超过则按100处理
   - pageNum最小值为1

4. **并发控制**: 
   - 发布、下线等操作建议使用乐观锁机制
   - 可在请求中携带version字段进行版本校验

5. **内容长度限制**:
   - 标题: 最大200字符
   - 内容: 建议不超过1MB

6. **软删除**: 删除操作采用软删除机制，数据库中保留记录但标记为deleted

---

**文档版本**: v1.0  
**最后更新**: 2026-06-11  
**生成工具**: kb-api-doc-gen Skill
