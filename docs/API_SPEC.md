# 知识库管理后台 API 接口规范

## 文档说明

本文档定义了知识库管理后台的RESTful API接口契约,包括分类管理、文档管理、版本管理等核心功能模块。

### 基础信息

- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **时间格式**: ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)

### 统一响应格式

所有接口均返回统一的Result<T>包装结构:

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
| code | Integer | 响应状态码,200表示成功,其他为错误码 |
| message | String | 响应消息,成功时为"success",失败时为错误描述 |
| data | Object/Array/null | 业务数据,根据具体接口而定 |

### 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 200 | 请求成功 | 200 |
| 400 | 参数错误 | 400 |
| 401 | 未授权 | 401 |
| 403 | 禁止访问 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |

### 枚举值定义

#### DocumentStatus (文档状态)

| 枚举值 | 说明 |
|--------|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| OFFLINE | 已下线 |

---

## 1. 分类管理接口

### 1.1 获取分类树形结构

**接口描述**: 获取所有分类的树形结构,用于前端展示分类层级关系

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/categories/tree`
- **Query参数**: 无

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
  ]
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类ID |
| parentId | Long/null | 父分类ID,根节点为null |
| name | String | 分类名称 |
| sortOrder | Integer | 排序序号,数值越小越靠前 |
| children | Array | 子分类列表,递归结构 |

---

### 1.2 新增分类

**接口描述**: 创建新的分类节点

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/categories`
- **Content-Type**: application/json

**请求体**:

```json
{
  "parentId": 1,
  "name": "移动开发",
  "sortOrder": 3
}
```

**请求字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parentId | Long/null | 否 | 父分类ID,null表示根节点 |
| name | String | 是 | 分类名称,长度1-100字符 |
| sortOrder | Integer | 是 | 排序序号,从1开始 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 5,
    "parentId": 1,
    "name": "移动开发",
    "sortOrder": 3,
    "createTime": "2026-06-07T12:00:00"
  }
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "分类名称不能为空",
  "data": null
}
```

---

### 1.3 修改分类

**接口描述**: 更新指定分类的信息(仅可修改名称和排序)

**请求**:
- **HTTP方法**: PUT
- **路径**: `/api/categories/{id}`
- **路径参数**: id - 分类ID

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
| name | String | 否 | 分类名称,长度1-100字符 |
| sortOrder | Integer | 否 | 排序序号 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "parentId": 1,
    "name": "前端技术",
    "sortOrder": 2,
    "updateTime": "2026-06-07T12:30:00"
  }
}
```

**错误响应示例**:

```json
{
  "code": 404,
  "message": "分类不存在",
  "data": null
}
```

---

### 1.4 删除分类

**接口描述**: 软删除指定分类(标记为已删除,不物理删除)

**请求**:
- **HTTP方法**: DELETE
- **路径**: `/api/categories/{id}`
- **路径参数**: id - 分类ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "该分类下存在子分类或文档,无法删除",
  "data": null
}
```

**注意事项**:
- 如果分类下存在子分类或关联文档,则不允许删除
- 采用软删除机制,数据库中保留记录但标记为deleted

---

## 2. 文档管理接口

### 2.1 分页查询文档列表

**接口描述**: 根据条件分页查询文档列表

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents`

**Query参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| categoryId | Long | 否 | - | 分类ID,筛选指定分类下的文档 |
| status | String | 否 | - | 文档状态:DRAFT/PUBLISHED/OFFLINE |
| title | String | 否 | - | 文档标题,支持模糊查询 |
| pageNum | Integer | 否 | 1 | 页码,从1开始 |
| pageSize | Integer | 否 | 10 | 每页数量,范围1-100 |

**请求示例**:

```
GET /api/documents?categoryId=1&status=PUBLISHED&title=Vue&pageNum=1&pageSize=10
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "list": [
      {
        "id": 101,
        "categoryId": 2,
        "categoryName": "前端开发",
        "title": "Vue3组件化开发最佳实践",
        "status": "PUBLISHED",
        "version": 3,
        "publishTime": "2026-06-05T10:30:00",
        "createTime": "2026-05-20T09:00:00",
        "createBy": "张三",
        "updateTime": "2026-06-05T10:30:00"
      },
      {
        "id": 102,
        "categoryId": 2,
        "categoryName": "前端开发",
        "title": "React Hooks使用指南",
        "status": "DRAFT",
        "version": 1,
        "publishTime": null,
        "createTime": "2026-06-01T14:20:00",
        "createBy": "李四",
        "updateTime": "2026-06-01T14:20:00"
      }
    ]
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Long | 总记录数 |
| list | Array | 当前页文档列表 |
| list[].id | Long | 文档ID |
| list[].categoryId | Long | 分类ID |
| list[].categoryName | String | 分类名称 |
| list[].title | String | 文档标题 |
| list[].status | String | 文档状态 |
| list[].version | Integer | 当前版本号 |
| list[].publishTime | String/null | 发布时间,ISO 8601格式 |
| list[].createTime | String | 创建时间,ISO 8601格式 |
| list[].createBy | String | 创建人 |
| list[].updateTime | String | 更新时间,ISO 8601格式 |

---

### 2.2 获取文档详情

**接口描述**: 获取指定文档的详细信息,包括完整内容

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}`
- **路径参数**: id - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 101,
    "categoryId": 2,
    "categoryName": "前端开发",
    "title": "Vue3组件化开发最佳实践",
    "content": "# Vue3组件化开发\n\n## 概述\n\nVue3引入了Composition API...",
    "status": "PUBLISHED",
    "version": 3,
    "publishTime": "2026-06-05T10:30:00",
    "createTime": "2026-05-20T09:00:00",
    "createBy": "张三",
    "updateTime": "2026-06-05T10:30:00",
    "updateBy": "张三"
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |
| categoryId | Long | 分类ID |
| categoryName | String | 分类名称 |
| title | String | 文档标题 |
| content | String | 文档内容(Markdown格式) |
| status | String | 文档状态 |
| version | Integer | 当前版本号 |
| publishTime | String/null | 发布时间 |
| createTime | String | 创建时间 |
| createBy | String | 创建人 |
| updateTime | String | 更新时间 |
| updateBy | String | 更新人 |

**错误响应示例**:

```json
{
  "code": 404,
  "message": "文档不存在",
  "data": null
}
```

---

### 2.3 创建文档草稿

**接口描述**: 创建新的文档草稿,初始状态为DRAFT

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents`
- **Content-Type**: application/json

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
| title | String | 是 | 文档标题,长度1-200字符 |
| content | String | 是 | 文档内容,Markdown格式 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 103,
    "categoryId": 2,
    "title": "TypeScript高级类型技巧",
    "status": "DRAFT",
    "version": 1,
    "createTime": "2026-06-07T12:00:00",
    "createBy": "王五"
  }
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "标题不能为空",
  "data": null
}
```

---

### 2.4 更新文档

**接口描述**: 更新文档信息,仅草稿状态的文档可以更新

**请求**:
- **HTTP方法**: PUT
- **路径**: `/api/documents/{id}`
- **路径参数**: id - 文档ID

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
| title | String | 否 | 文档标题 |
| content | String | 否 | 文档内容 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 103,
    "title": "TypeScript高级类型技巧(修订版)",
    "status": "DRAFT",
    "version": 1,
    "updateTime": "2026-06-07T12:30:00",
    "updateBy": "王五"
  }
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "只有草稿状态的文档可以更新",
  "data": null
}
```

**注意事项**:
- 只有状态为DRAFT的文档可以更新
- 已发布(PUBLISHED)或已下线(OFFLINE)的文档不可直接更新,需通过版本回滚或重新创建草稿

---

### 2.5 发布文档

**接口描述**: 发布文档,将草稿状态变更为已发布,并创建新版本

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/publish`
- **路径参数**: id - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 103,
    "title": "TypeScript高级类型技巧(修订版)",
    "status": "PUBLISHED",
    "version": 2,
    "publishTime": "2026-06-07T13:00:00",
    "updateTime": "2026-06-07T13:00:00"
  }
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "只有草稿状态的文档可以发布",
  "data": null
}
```

**注意事项**:
- 只有状态为DRAFT的文档可以发布
- 发布后版本号自动+1
- 发布时记录publishTime

---

### 2.6 下线文档

**接口描述**: 下线已发布的文档,状态变更为OFFLINE

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/offline`
- **路径参数**: id - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 103,
    "title": "TypeScript高级类型技巧(修订版)",
    "status": "OFFLINE",
    "version": 2,
    "updateTime": "2026-06-07T14:00:00"
  }
}
```

**错误响应示例**:

```json
{
  "code": 400,
  "message": "只有已发布的文档可以下线",
  "data": null
}
```

**注意事项**:
- 只有状态为PUBLISHED的文档可以下线
- 下线后文档不再对外展示,但保留历史版本

---

### 2.7 删除文档

**接口描述**: 软删除指定文档(标记为已删除,不物理删除)

**请求**:
- **HTTP方法**: DELETE
- **路径**: `/api/documents/{id}`
- **路径参数**: id - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应示例**:

```json
{
  "code": 404,
  "message": "文档不存在",
  "data": null
}
```

**注意事项**:
- 采用软删除机制,数据库中保留记录但标记为deleted
- 删除后文档及其所有版本均不可见

---

## 3. 版本管理接口

### 3.1 获取文档版本列表

**接口描述**: 获取指定文档的所有历史版本列表

**请求**:
- **HTTP方法**: GET
- **路径**: `/api/documents/{id}/versions`
- **路径参数**: id - 文档ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "versionId": 301,
      "version": 3,
      "title": "Vue3组件化开发最佳实践",
      "content": "# Vue3组件化开发\n\n## 最新版本内容...",
      "createTime": "2026-06-05T10:30:00",
      "createBy": "张三"
    },
    {
      "versionId": 302,
      "version": 2,
      "title": "Vue3组件化开发",
      "content": "# Vue3组件化开发\n\n## 第二版内容...",
      "createTime": "2026-05-28T15:20:00",
      "createBy": "张三"
    },
    {
      "versionId": 303,
      "version": 1,
      "title": "Vue3入门",
      "content": "# Vue3入门\n\n## 初始版本内容...",
      "createTime": "2026-05-20T09:00:00",
      "createBy": "张三"
    }
  ]
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | Long | 版本ID |
| version | Integer | 版本号,从1开始递增 |
| title | String | 该版本的标题 |
| content | String | 该版本的内容 |
| createTime | String | 版本创建时间,ISO 8601格式 |
| createBy | String | 版本创建人 |

**错误响应示例**:

```json
{
  "code": 404,
  "message": "文档不存在",
  "data": null
}
```

---

### 3.2 版本回滚

**接口描述**: 将文档回滚到指定的历史版本,创建新的草稿

**请求**:
- **HTTP方法**: POST
- **路径**: `/api/documents/{id}/rollback/{versionId}`
- **路径参数**: 
  - id - 文档ID
  - versionId - 要回滚到的版本ID

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 101,
    "title": "Vue3组件化开发",
    "status": "DRAFT",
    "version": 4,
    "createTime": "2026-06-07T15:00:00",
    "createBy": "李四"
  }
}
```

**错误响应示例**:

```json
{
  "code": 404,
  "message": "版本不存在",
  "data": null
}
```

**注意事项**:
- 回滚操作会基于指定版本创建一个新的草稿
- 新版本号在当前最大版本号基础上+1
- 回滚后的文档状态为DRAFT,需要重新发布才能生效
- 原已发布的版本保持不变,作为历史记录保留

---

## 附录

### A. 完整请求/响应流程示例

#### 场景: 创建并发布一篇新文档

1. **创建草稿**:
   ```
   POST /api/documents
   Body: {"categoryId": 2, "title": "测试文档", "content": "# 测试"}
   Response: {"code": 200, "data": {"id": 200, "status": "DRAFT", "version": 1}}
   ```

2. **更新草稿**(可选):
   ```
   PUT /api/documents/200
   Body: {"title": "测试文档(修订)", "content": "# 测试修订版"}
   Response: {"code": 200, "data": {"status": "DRAFT"}}
   ```

3. **发布文档**:
   ```
   POST /api/documents/200/publish
   Response: {"code": 200, "data": {"status": "PUBLISHED", "version": 2}}
   ```

4. **查看版本历史**:
   ```
   GET /api/documents/200/versions
   Response: {"code": 200, "data": [{"version": 2, ...}, {"version": 1, ...}]}
   ```

5. **下线文档**(如需):
   ```
   POST /api/documents/200/offline
   Response: {"code": 200, "data": {"status": "OFFLINE"}}
   ```

### B. 数据库设计建议

#### categories表
```sql
CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 1,
  deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME,
  INDEX idx_parent_id (parent_id)
);
```

#### documents表
```sql
CREATE TABLE documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 1,
  publish_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  create_by VARCHAR(50),
  create_time DATETIME NOT NULL,
  update_by VARCHAR(50),
  update_time DATETIME,
  INDEX idx_category_id (category_id),
  INDEX idx_status (status),
  INDEX idx_create_time (create_time)
);
```

#### document_versions表
```sql
CREATE TABLE document_versions (
  version_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  version INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT,
  create_by VARCHAR(50),
  create_time DATETIME NOT NULL,
  INDEX idx_document_id (document_id),
  INDEX idx_version (document_id, version)
);
```

### C. 接口调用注意事项

1. **认证授权**: 所有接口需要在请求头中携带认证token
   ```
   Authorization: Bearer <token>
   ```

2. **幂等性**: 
   - GET、DELETE接口具有幂等性
   - POST、PUT接口不保证幂等性,客户端需做好防重复提交

3. **分页限制**: 
   - pageSize最大值为100,超过则按100处理
   - pageNum最小值为1

4. **并发控制**: 
   - 发布、下线等操作建议使用乐观锁机制
   - 可在请求中携带version字段进行版本校验

5. **内容长度限制**:
   - 标题: 最大200字符
   - 内容: 建议不超过1MB

---

**文档版本**: v1.0  
**最后更新**: 2026-06-07  
**维护者**: API设计团队
