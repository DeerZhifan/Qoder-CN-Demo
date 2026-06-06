# CDP 自定义表单引擎 设计手册

> 对应使用手册：[cdp-module-form.md](../3-组件使用手册/cdp-module-form.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

企业信息化系统中存在大量"增删改查 + 审批"的业务表单，如果每个表单都由开发人员硬编码实现，开发效率低且变更成本高。自定义表单引擎的目标是让运营人员通过可视化配置完成表单定义和列表页面设计，减少重复编码。

`leatop-cdp-business-form` 模块的设计目标是：

1. **元数据驱动**：通过 `FormMetaObject`（表对象）和 `FormMetaField`（字段）描述数据结构，表单渲染和数据存取均基于元数据动态执行。
2. **设计态与运行态分离**：设计态由 `DesignForm`（表单设计器）和 `FormDesignList`（列表设计器）管理布局和交互配置；运行态由 `FormRecord` 驱动实际数据的读写。
3. **多端适配**：表单布局支持 PC 端和移动端两套配置，通过 `platformType` 参数区分。
4. **与工作流集成**：表单可绑定工作流，实现"表单 + 审批流"的完整业务闭环。

## 二、整体架构

模块遵循 CDP 标准五层结构：

```
leatop-cdp-business-form/
  form-api            # 接口契约：Business 接口（@FeignClient）、DTO、QO
  form-service        # 核心实现：元数据管理、表单设计、数据读写、工作流集成
  form-controller     # REST 端点
  form-boot-starter   # 单体模式自动配置
  form-cloud-starter  # 微服务模式自动配置
```

核心数据流分为设计态和运行态：

**设计态**：管理员 → `DesignFormBusiness` / `FormMetaObjectBusiness` / `FormDesignListBusiness` → 配置元数据、表单布局、列表布局 → 持久化到数据库

**运行态**：终端用户 → `FormRecordBusiness.listRecord()` / `saveOrUpdate()` → `FormRecordService` → 基于元数据动态构建 SQL → `JdbcTemplate` 直接操作业务表

> 设计决策：运行态使用 `JdbcTemplate` 直接操作业务表而非通过 MyBatis-Plus 的实体映射。这是因为表单对应的数据库表是动态的（由元数据定义），无法预先生成 PO 类。`JdbcTemplate` 配合动态 SQL 拼接是此场景下的合理选择。

## 三、关键类说明

| 类名 | 职责 |
|------|------|
| `FormMetaObjectBusiness` / `FormMetaObjectService` | 元数据对象管理，定义表单对应的数据库表结构，支持同步（`syncById`）、导入导出 |
| `FormMetaFieldBusiness` / `FormMetaFieldService` | 元数据字段管理，定义表中各列的类型、校验规则、显示名称 |
| `DesignFormBusiness` / `FormDesignFormService` | 表单设计器，管理表单布局（`layout` / `layoutColumns`）、支持按元对象自动生成默认表单 |
| `DesignFormButtonBusiness` / `FormDesignFormButtonService` | 表单按钮配置（提交、保存、重置等） |
| `FormDesignListBusiness` / `FormDesignListService` | 列表设计器，配置列表页的查询条件、数据列 |
| `FormDesignListButtonBusiness` / `FormDesignListButtonService` | 列表按钮配置 |
| `FormDesignListColumnBusiness` / `FormDesignListColumnService` | 列表列配置 |
| `FormRecordBusiness` / `FormRecordService` | 运行态核心，负责表单数据的查询（支持 `FilterField` 动态过滤）、新增/更新、删除、导出 |
| `FormValueSourceBusiness` / `FormValueSourceService` | 表单值来源配置，支持静态值、字典、接口等多种数据源 |
| `FormDatabaseBusiness` / `FormDatabaseService` | 表单关联的数据库配置管理 |
| `FormCatalogBusiness` / `FormCatalogService` | 表单分类目录管理 |
| `FormReportBusiness` / `FormReportService` | 表单报表功能 |
| `FormWorkflowService` / `FormWorkflowController` | 表单与工作流集成，处理审批流程绑定 |
| `FormConfig` | 模块配置类，注册 `JdbcTemplate` Bean 用于动态 SQL 执行 |
| `AbstractTenantService` | 多租户抽象基类，提供租户上下文感知能力 |
| `FilterField` / `OrderField` | 运行态查询参数模型，定义过滤条件和排序规则 |
| `Dict` | 常量定义类，包含如 `KEY_RECORD_QUERY` 等约定键名 |
| `ParameterUtils` | 参数转换工具，将前端请求参数转为 `FilterField` 列表 |
| `DBTableMetaGenerateDTO` | 数据库表元数据生成 DTO，用于从现有数据库表反向生成元对象 |

## 四、扩展机制

### 4.1 自定义值来源

`FormValueSourceService` 管理表单字段的值来源配置。当内置的静态值和字典无法满足需求时，可配置接口类型的值来源，通过 HTTP 调用获取动态数据。新增值来源类型只需在配置中添加对应的获取策略。

### 4.2 元数据导入导出

`FormMetaObjectService` 提供 `exportMetaData()` 和 `importMetaData()` 方法，支持将元对象、表单设计、列表设计等配置打包为 JSON 导出，在其他环境中导入。这为表单配置的跨环境迁移提供了便利。

### 4.3 表单自动生成

`FormDesignFormService.generateDefaultForms()` 可根据元对象定义自动生成默认的表单布局和列表配置，开发者在此基础上微调即可，大幅减少初始配置工作量。

### 4.4 工作流集成

`FormWorkflowService` 和 `FormWorkflowController` 提供表单与工作流引擎的桥接能力。表单提交时可自动触发审批流程，审批结果回写到表单记录状态。这使得"低代码表单 + 流程审批"的组合成为可能。

### 4.5 多端布局复制

`FormDesignFormService.copyFormDesignConfig()` 支持将 PC 端的表单布局复制到移动端（或反向），避免重复配置。通过 `platformType` 参数（0=PC，1=移动端）区分不同端的布局存储。
