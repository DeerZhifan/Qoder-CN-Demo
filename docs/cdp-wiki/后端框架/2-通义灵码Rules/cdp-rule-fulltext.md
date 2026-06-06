---
trigger: when_referenced
knowledge_source:
  - cdp-design-fulltext
  - cdp-module-fulltext
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-fulltext-boot-starter` 或 `leatop-cdp-business-fulltext-cloud-starter` 依赖
- 使用 `DataQueryBusiness`、`DataAsyncBusiness` 接口
- 使用 `IdUtil` 生成索引名或文档 ID
- 配置 Elasticsearch 连接（`ElasticSearchProperties`）
- 激活 `fulltext` profile

---

## 前置依赖

1. Maven 依赖（二选一）：

```xml
<!-- 单体部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-fulltext-boot-starter</artifactId>
</dependency>

<!-- 微服务部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-fulltext-cloud-starter</artifactId>
</dependency>
```

2. 激活 `fulltext` profile：

```yaml
spring:
  profiles:
    active: fulltext
```

3. 部署并配置 Elasticsearch 集群，在配置中指定连接地址。

---

## 配置要点

### 索引命名规范

- **索引名**：`cdp_{systemCode}_{module}`（全小写），如 `cdp_oa_document`
- **跨模块检索**：仅指定 systemCode 时，`IdUtil.genQueryIndexName()` 生成 `cdp_{systemCode}_*` 通配符索引模式

### 文档 ID 生成规则

| 文档类型 | ID 格式 |
|---------|--------|
| 业务数据（budata） | `{indexName}_{budataId}` |
| 工作流（workflow） | `{indexName}_{budataId}_workflow_{taskId}` |
| 附件（attach） | `{indexName}_{budataId}_attach_{attachId}` |
| 权限（labelperm） | `{indexName}_{budataId}_perm_{labelType}_{labelValue}` |

### 权限模型

查询时权限过滤通过 ES `should` + `minimumShouldMatch` 在查询层实现，满足以下任一条件即可访问：

| 权限字段 | 说明 |
|---------|------|
| `hasPublic` | 是否公开（1=全网公开） |
| `permUserIds` | 有权限的用户 ID 数组 |
| `permDepartmentIds` | 有权限的部门 ID 数组 |
| `permCompanyIds` | 有权限的公司 ID 数组 |

### 查询模式

- `matchQueryField` 决定搜索范围：标题、全文、含附件
- `matchMode` 决定匹配方式：`matchQuery`（全文匹配）或 `matchPhrasePrefixQuery`（精确前缀匹配）
- 高级查询支持 12 种比较运算符（EQUAL、LIKE、BETWEEN 等），基于 Nested Query 查询 `advancedSearchFields`

---

## 代码模式

### 推荐写法

**通过 DataQueryBusiness 执行检索查询：**

<!-- TODO: 补充代码示例 -->

**通过 DataAsyncBusiness 同步数据到 ES：**

<!-- TODO: 补充代码示例 -->

**使用 IdUtil 生成索引名和文档 ID：**

<!-- TODO: 补充代码示例 -->

**自定义同步策略（扩展 SendData 接口）：**

实现 `SendData` 接口并遵循命名约定 `SendData{操作名}`，`SendDataFactory` 通过 Spring 自动发现机制加载。

<!-- TODO: 补充代码示例 -->

### 禁止事项

- **禁止业务代码直接操作 Elasticsearch** -- 必须通过 `DataQueryBusiness` 和 `DataAsyncBusiness` 统一接口，不得直接构造 `RestHighLevelClient` 请求
- **禁止自定义索引名和文档 ID 格式** -- 必须使用 `IdUtil` 工具类生成，遵循 `cdp_{systemCode}_{module}` 命名约定，避免跨项目索引冲突
- **禁止在应用层过滤权限** -- 权限过滤在 ES 查询层面实现（`should` + `minimumShouldMatch`），应用层过滤会导致分页计数不准确
- **禁止无关键字时执行全量 matchAll 查询** -- 框架对空关键字做了防御性设计，浏览全部数据应通过列表管理接口而非全文检索接口
- **禁止将 DataTool 重量级依赖引入检索服务** -- `fulltext-datatool` 包含 Canal Client、XXL-Job 执行器等依赖，仅在数据同步场景使用，不应污染检索服务类路径
- **禁止手动设置父子文档路由** -- 框架通过 `IndexRequest.routing()` 自动设置路由值为父文档 ID，手动设置会导致父子文档分片不一致
- **禁止混用 Nested Object 和 Parent-Child** -- 框架使用 ES 原生 Join field 实现父子关联，不要对同一文档结构混用 Nested 方式
