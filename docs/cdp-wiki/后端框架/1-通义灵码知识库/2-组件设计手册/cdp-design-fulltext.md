# CDP 全文检索 设计手册

> 对应使用手册：[cdp-module-fulltext.md](../3-组件使用手册/cdp-module-fulltext.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

传统关系型数据库的 LIKE 查询在面对大规模文本检索时性能急剧下降，且不支持分词、相关性排序、高亮等高级功能。企业信息化系统中，用户常常需要跨模块、跨系统地检索业务数据、工作流任务和附件内容。如果每个业务模块自行对接 Elasticsearch，会导致索引管理混乱、权限控制缺失、数据同步方案碎片化。

`leatop-cdp-business-fulltext` 模块的设计目标是：

1. **统一检索入口**：提供通用的全文检索 API，业务方无需直接操作 Elasticsearch。
2. **多维度数据模型**：支持业务数据、工作流数据、附件内容、权限标签四种文档类型，通过 Parent-Child 关系关联。
3. **权限内嵌**：检索结果自动过滤无权限数据，支持用户、部门、公司、标签和流程参与人五种权限维度。
4. **数据同步可管理**：提供同步任务管理和独立的 DataTool 工具，支持增量和全量同步。
5. **索引规范化**：统一索引命名和文档 ID 生成规则，避免跨项目索引冲突。

## 二、整体架构

模块由六个子模块组成：

```
leatop-cdp-business-fulltext/
  fulltext-api            # 接口契约：Business 接口（@FeignClient）、DTO、QO、枚举、ID 生成工具
  fulltext-service        # 核心实现：ES 操作、查询构建、同步逻辑、配置管理
  fulltext-controller     # REST 端点
  fulltext-datatool       # 独立数据同步工具：批量索引、Canal 监听、XXL-Job 集成
  fulltext-boot-starter   # 单体模式自动配置
  fulltext-cloud-starter  # 微服务模式自动配置
```

> 设计决策：将 DataTool 独立为子模块而非合并到 service 中。DataTool 包含 Canal Client、XXL-Job 执行器等重量级依赖，仅在数据同步场景使用，不应污染检索服务的类路径。独立部署还允许同步和检索服务分别扩缩容。

核心数据流分为两条路径：

**索引路径**：业务系统 / DataTool → `DataAsyncBusiness` → `DataAsyncBusinessImpl` → `RestHighLevelClient` (BulkRequest) → Elasticsearch

**查询路径**：前端/业务系统 → `DataQueryBusiness` → `DataQueryBusinessImpl` → 构建 BoolQuery + 权限过滤 + 高亮 → `RestHighLevelClient` (SearchRequest) → 解析 SearchResponse → `DataQueryDto`

## 三、核心设计模式

### 3.1 Composite Document 模式：四种文档类型的 Parent-Child 关联

索引中的文档通过 ES 的 Join 字段（`budataJoin`）建立父子关系：

- **budata**（父文档）—— 业务数据，包含标题、正文、URL、标签等核心检索字段。
- **workflow**（子文档）—— 工作流任务数据，关联到父文档的 budataId。
- **attach**（子文档）—— 附件数据，包含附件名和附件内容的全文。
- **labelperm**（子文档）—— 权限标签数据，记录标签类型和有权限的用户列表。

`DataAsyncBusinessImpl` 在索引时通过 `IndexRequest.routing()` 设置路由值为父文档 ID，确保父子文档落在同一分片上。`DataQueryBusinessImpl` 在查询时使用 `HasChildQueryBuilder` 和 `HasParentQueryBuilder` 实现跨层级检索——例如"搜索内容+附件"模式下，用 `HasChildQueryBuilder` 将命中附件子文档的父文档一并召回。

> 设计决策：选择 ES 原生 Parent-Child（Join field）而非 Nested Object。Parent-Child 允许父子文档独立更新，当附件或权限变化时无需重建整个父文档。代价是 Join 查询的性能低于 Nested，但在权限标签频繁变更的企业场景下，更新效率优先。

### 3.2 Convention-Based ID 模式：规范化的索引名和文档 ID

`IdUtil` 工具类通过约定规则生成索引名和文档 ID：

- **索引名**：`cdp_{systemCode}_{module}`（全小写），如 `cdp_oa_document`。
- **文档 ID**：`{indexName}_{budataId}[_{type}_{subId}]`。业务数据仅含 budataId，工作流追加 `_workflow_{taskId}`，附件追加 `_attach_{attachId}`，权限追加 `_perm_{labelType}_{labelValue}`。

`genQueryIndexName()` 方法支持通配符查询——当只指定 systemCode 不指定 module 时，生成 `cdp_{systemCode}_*`，利用 ES 的索引模式匹配实现跨模块检索。

### 3.3 Builder 模式：多层查询条件组装

`DataQueryBusinessImpl` 将复杂的 ES 查询拆分为四个独立的构建方法，逐步组装 `BoolQueryBuilder`：

1. `buildKeyWordQuery()` —— 关键字查询，根据 `matchQueryField` 决定搜索范围（标题/全文/含附件），根据 `matchMode` 选择 `matchQuery`（全文）或 `matchPhrasePrefixQuery`（精确前缀）。
2. `buildFilterQuery()` —— 基础过滤，支持按标签和时间范围（天/周/月/年）过滤。
3. `buildPermQuery()` —— 权限过滤，构建 `should` 组合查询（公开 OR 用户 OR 部门 OR 公司 OR 标签权限 OR 流程参与人），至少满足一项。
4. `buildAdvancedQuery()` —— 高级字段查询，支持 12 种比较运算符（等于、模糊、范围、IN 等），基于 Nested Query 查询 `advancedSearchFields` 嵌套字段。

### 3.4 Factory 模式：DataTool 同步操作分发

`SendDataFactory` 通过 Spring 的 `Map<String, SendData>` 自动注入机制收集所有 `SendData` 实现 Bean。`send()` 方法根据接口名（如 `BudataUpdate`、`AttachDelete`）动态查找对应的实现类（`SendDataBudataUpdate`、`SendDataAttachDelete`），实现同步操作的策略分发。命名约定为 `SendData` + 首字母大写的接口名。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `DataQueryBusiness` | 全文检索查询接口，`@FeignClient` 标注，定义列表查询和分页查询 |
| `DataAsyncBusiness` | 数据同步接口，定义业务数据、工作流、附件、权限的批量增删改操作 |
| `DataQueryBusinessImpl` | 查询核心实现，构建 BoolQuery、权限过滤、高亮、排序、结果解析 |
| `DataAsyncBusinessImpl` | 同步核心实现，使用 `BulkRequest` 批量操作 ES，支持按查询删除/更新 |
| `ApplicationBusiness` / `ApplicationModuleBusiness` | 检索应用和模块的管理接口 |
| `DatasourceBusiness` | 数据源管理接口，配置同步数据的来源 |
| `SyncJobBusiness` / `SyncJobStepBusiness` | 同步任务和步骤的管理接口 |
| `SyncJobLogBusiness` / `SyncJobStepLogBusiness` | 同步执行日志接口 |
| `ModuleQueryBusiness` | 模块检索配置管理接口 |
| `ModulePermBusiness` | 模块权限配置管理接口 |
| `LabelBusiness` | 检索标签管理接口 |
| `IdUtil` | 索引名和文档 ID 生成工具，封装命名约定 |
| `FulltextEnum.DataType` | 数据类型枚举：BUDATA(0)、WORKFLOW(1)、ATTACH(2)、PERM(3) |
| `ElasticSearchConfig` | ES 连接配置类，基于 `ElasticSearchProperties` 创建 `RestHighLevelClient` Bean |
| `ElasticSearchProperties` | ES 连接属性类，支持多节点地址和用户名密码认证 |
| `DataToolBusiness` / `DataToolBusinessImpl` | DataTool 批量索引业务逻辑 |
| `SendData` | 同步操作策略接口，定义 `send(List<Map>)` 方法 |
| `SendDataFactory` | 同步操作工厂，根据接口名动态分发到具体 SendData 实现 |
| `FileContentReader` | 附件内容读取器，用于提取文件文本内容建立索引 |
| `CanalClient` / `CanalCommand` | Canal 增量同步客户端，监听数据库 binlog 变更 |
| `CompareType` | 高级查询比较运算符枚举（EQUAL、LIKE、BETWEEN 等 12 种） |

## 五、扩展机制

### 5.1 自定义权限维度

当前权限过滤支持五种维度（公开、用户、部门、公司、标签权限）。如需新增维度（如项目组），只需：
1. 在索引 Mapping 中添加对应数组字段。
2. 在 `DataQueryBusinessImpl.buildPermQuery()` 中增加 `should` 子句。
3. 在同步接口中传入新维度数据。

### 5.2 自定义同步策略

DataTool 的 `SendData` 接口是开放的扩展点。新增同步操作只需创建实现类并遵循命名约定 `SendData{操作名}`，`SendDataFactory` 会通过 Spring 自动发现机制加载。同时，DataTool 集成了 XXL-Job（`XxlJobConfig`），可将同步操作注册为定时任务。

### 5.3 Canal 增量同步

`fulltext-datatool` 子模块内置 Canal Client（`CanalClient`），可监听数据库 binlog 变更并实时同步到 ES。通过 `CanalConfig` 配置 Canal 服务地址和监听的表，实现近实时的增量索引更新。

## 六、模块协作

- **leatop-cdp-business-file**：附件内容索引依赖文件模块提供的文件读取能力，`FileContentReader` 负责从文件系统提取文本。
- **leatop-cdp-business-workflow**：工作流数据同步到 ES 后，检索结果中的流程任务信息（处理人、办理状态）来自工作流模块的数据。
- **leatop-cdp-business-datascope**：权限过滤中的用户/部门/公司 ID 来自数据权限模块提供的上下文。
- **leatop-cdp-base-job**：DataTool 通过 XXL-Job 执行器调度全量和增量同步任务。

## 七、设计权衡与约束

> 设计决策：使用 `RestHighLevelClient` 而非新版 Java API Client。`RestHighLevelClient` 在 ES 7.x 生态中更成熟且社区资料丰富，虽然已被标记为 deprecated，但对于当前 ES 版本仍然稳定。未来升级 ES 8.x 时需要迁移到新 API。

> 设计决策：权限过滤在 ES 查询层面实现（通过 `should` + `minimumShouldMatch`），而非在应用层过滤。这保证了分页数据的正确性——如果在应用层过滤，分页计数会不准确。代价是查询复杂度增加，但借助 ES 的 filter context（不参与评分），性能影响可控。

> 设计决策：关键字为空时查询条件设为 `budataId = '78945687542136'`（一个不存在的 ID），等同于返回空结果。这是一个防御性设计，避免无关键字时执行全量 `matchAll` 导致大量数据返回。如果业务需要浏览全部数据的场景，应通过列表管理接口而非全文检索接口。

> 设计决策：DataTool 作为独立子模块而非微服务。当前同步工具以库的形式提供，由业务方自行集成。如果同步吞吐量成为瓶颈，可将 DataTool 独立部署为微服务，通过消息队列接收同步请求（`MqDataDto` 已预留了消息队列数据传输结构）。
