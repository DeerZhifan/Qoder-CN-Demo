# 如何使用 CDP 全文检索组件

## 概述

全文检索组件（`leatop-cdp-business-fulltext`）基于 Elasticsearch 实现，提供统一的索引管理、数据同步、权限控制和全文搜索能力。支持业务数据、工作流数据和附件内容的全文检索。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-fulltext-boot-starter</artifactId>
</dependency>
```

激活 fulltext profile：

```yaml
spring:
  profiles:
    active: fulltext
```

## 核心概念

### 索引规范

- **索引模板**：统一的 ES 索引模板 `cdp_template`
- **索引命名规则**：`cdp_{项目编号}_{模块编号}`
- **文档 ID 规则**：
  - 业务数据：`{索引名}_{budataId}`
  - 工作流：`{索引名}_{budataId}_workflow_{taskId}`
  - 附件：`{索引名}_{budataId}_attach_{attachId}`
  - 权限：`{索引名}_{budataId}_perm_{labelType}_{labelValue}`

### 权限模型

| 权限字段 | 说明 |
|---------|------|
| `hasPublic` | 是否公开（1=全网公开） |
| `permUserIds` | 有权限的用户 ID 数组 |
| `permDepartmentIds` | 有权限的部门 ID 数组 |
| `permCompanyIds` | 有权限的公司 ID 数组 |

查询时满足任一权限条件即可访问。

## 核心功能

### 管理后台

- **应用管理（Application）**：配置检索应用和模块
- **数据源管理（Datasource）**：配置数据同步的来源
- **同步任务（SyncJob）**：定义和管理数据同步任务
- **标签管理（Label）**：管理检索标签和分类

### 数据同步

- **SyncJob**：同步任务定义
- **SyncJobStep**：同步任务步骤
- **SyncJobLog / SyncJobStepLog**：同步执行日志

### 数据查询

- **DataQuery**：通用数据检索接口
- **ModuleQuery**：按模块检索
- **ModulePerm**：模块权限控制

### 数据工具

- **DataTool**：全文检索数据工具（`leatop-cdp-business-fulltext-datatool`），用于批量数据索引

## 注意事项

> 注意：全文检索依赖 Elasticsearch 服务，需先部署 ES 集群并在配置中指定连接地址。

> 注意：权限模型支持多维度（用户、部门、公司、标签），查询时自动拼装权限过滤条件。

> 注意：数据同步任务支持增量和全量两种模式，生产环境建议使用增量同步减少 ES 压力。
