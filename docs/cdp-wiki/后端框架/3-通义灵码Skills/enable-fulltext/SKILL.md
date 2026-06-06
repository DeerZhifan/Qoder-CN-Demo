# 启用 CDP 全文检索组件

## 描述

在已有 CDP 项目中启用全文检索组件（`leatop-cdp-business-fulltext`），基于 Elasticsearch 提供统一的索引管理、数据同步、权限控制和全文搜索能力。支持业务数据、工作流数据、附件内容的全文检索，检索结果自动过滤无权限数据。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体部署 或 `cloud` 微服务部署，默认 `boot`）
3. **Elasticsearch 连接地址**（如 `172.17.1.28:9200`，支持多节点逗号分隔）
4. **ES 认证信息**（用户名和密码，如不需要认证可跳过）
5. **项目编号**（systemCode，用于索引命名，如 `oa`）
6. **模块编号**（module，用于索引命名，如 `document`）

---

## 步骤 1：添加 Maven 依赖

> 根据部署模式引入对应的 Starter。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加（二选一）：

**单体部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-fulltext-boot-starter</artifactId>
</dependency>
```

**微服务部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-fulltext-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：激活 fulltext Profile

> 全文检索组件通过 Spring Profile 机制激活。

在 `application.yaml` 中添加或追加 `fulltext` 到 `spring.profiles.active`：

```yaml
spring:
  profiles:
    active: fulltext
```

## 步骤 3：配置 Elasticsearch 连接

> 配置 ES 集群连接地址和认证信息。支持多节点部署，多个地址用逗号分隔。

在 `application-fulltext.yaml`（或对应的 profile 文件）中配置：

```yaml
elasticsearch:
  hosts: {ES连接地址}          # 如 172.17.1.28:9200，多节点用逗号分隔
  username: {用户名}           # 无认证时可省略
  password: {密码}             # 无认证时可省略
```

## 步骤 4：了解索引命名规范

> 索引名和文档 ID 由框架 `IdUtil` 工具类统一生成，业务代码不要自行拼接。

- **索引名规则**：`cdp_{项目编号}_{模块编号}`，如 `cdp_oa_document`
- **文档 ID 规则**：
  - 业务数据：`{索引名}_{budataId}`
  - 工作流：`{索引名}_{budataId}_workflow_{taskId}`
  - 附件：`{索引名}_{budataId}_attach_{attachId}`
  - 权限：`{索引名}_{budataId}_perm_{labelType}_{labelValue}`
- **跨模块检索**：仅指定 systemCode 时生成 `cdp_{systemCode}_*` 通配符

## 步骤 5：配置检索应用和模块

> 通过管理后台配置检索应用、模块、数据源和同步任务。

1. **应用管理（Application）**：创建检索应用，关联项目编号
2. **模块管理（ApplicationModule）**：在应用下创建模块，关联模块编号
3. **数据源管理（Datasource）**：配置数据同步的来源数据库
4. **同步任务（SyncJob）**：定义数据同步任务和步骤

## 步骤 6：使用数据查询接口

> 通过 `DataQueryBusiness` 接口执行全文检索。权限过滤在 ES 查询层自动拼装，无需业务代码处理。

<!-- TODO: 补充代码示例 -->

## 步骤 7：使用数据同步接口（可选）

> 通过 `DataAsyncBusiness` 接口将业务数据同步到 ES。支持业务数据、工作流、附件、权限标签四种文档类型的批量增删改。

<!-- TODO: 补充代码示例 -->

## 步骤 8：验证

启动应用，检查以下内容：

1. 控制台无 Elasticsearch 连接异常
2. 日志中出现 ES 连接成功信息
3. 通过管理后台创建检索应用和模块，配置数据同步任务
4. 执行数据同步后，通过检索接口查询数据，确认返回结果和权限过滤正常

---

## 完成后提醒

1. 全文检索依赖 Elasticsearch 服务，需先部署 ES 集群
2. 不要直接操作 `RestHighLevelClient`，统一通过 `DataQueryBusiness` 和 `DataAsyncBusiness` 接口
3. 索引名和文档 ID 必须通过 `IdUtil` 工具类生成，遵循命名约定避免索引冲突
4. 权限过滤在 ES 查询层实现，不要在应用层二次过滤，否则分页计数不准确
5. 数据同步支持增量和全量两种模式，生产环境建议使用增量同步减少 ES 压力
6. `fulltext-datatool` 子模块包含 Canal Client 和 XXL-Job 等重量级依赖，仅在数据同步场景引入，不要污染检索服务类路径
7. 当前使用 `RestHighLevelClient`（ES 7.x），未来升级 ES 8.x 需迁移到新 Java API Client
