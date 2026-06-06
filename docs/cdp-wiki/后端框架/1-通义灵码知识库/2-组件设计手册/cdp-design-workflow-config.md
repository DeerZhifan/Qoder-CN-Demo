# CDP 工作流配置管理 设计手册

> 对应使用手册：[cdp-module-workflow-config.md](../3-组件使用手册/cdp-module-workflow-config.md)

## 一、设计目标与背景

工作流配置管理为流程设计器提供底层数据支撑，覆盖流程模板、环节模板、变量模板、事件配置、参与者配置等所有设计时元数据。设计目标：

- 提供完整的流程设计元数据 CRUD，支持可视化流程设计器的后端需求。
- 配置数据按"流程模板 -> 环节模板 -> 事件/变量/路由"的层次组织，结构清晰。
- 支持流程复用：通过子流程（Callprocess）机制实现模板间的调用和嵌套。
- 配置变更不影响已运行实例，仅对新发起的流程生效。

> 设计决策：配置管理的所有 Business 接口均继承 `BaseFlowBusiness`，获得统一的分页查询和 CRUD 能力。这使得前端流程设计器可以用一套通用组件对接所有配置实体，降低前后端联调成本。

## 二、整体架构

配置实体按功能域分为五组：

```
[流程结构配置]
  ConfigBusiness         -- 扩展属性配置，支持树形结构展示
  ActtemplateBusiness    -- 环节模板管理（审批人规则、时限、退回方式等）
  ActrelationBusiness    -- 环节路由关系
  ActvarBusiness         -- 环节变量（路由条件表达式）
  ActmutexBusiness       -- 互斥环节配置
  AssignbackactBusiness  -- 可退回环节配置
  VartemplateBusiness    -- 变量模板定义

[事件配置]
  EventBusiness          -- 事件定义
  EventvarBusiness       -- 事件变量
  ActeventBusiness       -- 环节-事件关联
  ActsubprocessBusiness  -- 子流程配置
  CallprocessBusiness    -- 主流程调用子流程

[参与者配置]
  TransactorBusiness     -- 办理人配置
  UserAssignDepartmentBusiness -- 用户部门分配
  UserWfBusiness / RoleWfBusiness / OrgBusiness -- 工作流用户/角色/组织

[任务模板配置]
  JobtemplateBusiness    -- 流程模板（顶层实体）
  JobtemplatefileBusiness -- 流程图文件

[辅助配置]
  TagBusiness / TagdetailBusiness      -- 标签管理
  CommentCategoryBusiness / CommentGroupBusiness -- 意见分类管理
  MsgTemplateBusiness    -- 消息模板
  OpiniontemplateBusiness -- 常用意见模板
  CompanypermBusiness    -- 公司权限配置
```

配置数据的核心关联关系：`JobtemplatePo`（流程模板）为顶层实体，通过 `jobTemplateId` 关联下属的 `ActtemplatePo`（环节）、`EventPo`（事件）等。`ConfigBusiness.viewTree()` 提供树形视图，按 `dataType`、`companyId`、`systemId`、`billId`、`jobTemplateId` 五个维度过滤配置树。

## 三、关键类说明

| 类 | 说明 |
|----|------|
| `ConfigBusiness` | 扩展属性配置接口。`viewTree()` 返回 `ConfigTreeDto` 树形结构，支持按多维度过滤 |
| `ActtemplateBusiness` | 环节模板管理。`getEventActTemplates()` 查询配置了指定事件的环节；`updateCallProcessType()` 修改子流程调用方式；`updateBackMethod()` 修改退回方式；`saveMutiList()` 批量保存环节及关联数据 |
| `VartemplateBusiness` | 变量模板管理，继承 `BaseFlowBusiness<VartemplateDto>` |
| `EventBusiness` / `EventvarBusiness` | 事件及事件变量管理 |
| `ActeventBusiness` | 环节-事件关联管理 |
| `TransactorBusiness` | 办理人配置管理 |
| `JobtemplateBusiness` | 流程模板管理 |
| `JobtemplatefileBusiness` | 流程图文件管理 |
| `TagBusiness` / `TagdetailBusiness` | 标签管理 |
| `CommentCategoryBusiness` / `CommentGroupBusiness` | 意见分类和分组管理 |
| `ActTemplatePath` | 环节路径封装（`bean` 包），包含 `startActRelationId`、`manualActTemplates`（人工环节列表）和 `decisionActTemplates`（决策环节列表） |
| `TemplateFull` | 流程模板完整快照（`model.copy` 包），用于模板复制 |
| `ConfigPo` / `ActtemplatePo` / `ActrelationPo` / `ActvarPo` | 对应的持久化对象 |

## 四、扩展机制

1. **自定义审批人策略**：`ActtemplateBusiness.updateUseCustomActor()` 控制环节是否由前端自行选择处理人员。当设为"后台配置"时，引擎根据 `TransactorBusiness` 的办理人配置自动分配；当设为"前端选择"时，通过 `IWorkflowPageEvent` 接口让业务系统动态提供候选人。

2. **子流程复用**：`ActsubprocessBusiness` 和 `CallprocessBusiness` 支持在主流程的某个环节调用另一个流程模板作为子流程。`ActtemplateBusiness.updateCallProcessType()` 配置子流程返回方式（同步等待/异步通知）。

3. **退回规则定制**：`AssignbackactBusiness` 定义每个环节可退回的目标环节列表。`ActtemplateBusiness.updateBackMethod()` 和 `updateBackShowWay()` 分别配置退回方式和退回页面显示方式。

4. **反馈机制**：`ActtemplateBusiness.updateFeedbackTrigger()` 配置反馈触发方式（节点完成触发或任务完成触发），对应 `WorkflowCallbackEventArgs.feedbackTrigger` 字段。

5. **模板复制**：`TemplateFull`（`model.copy` 包）封装了流程模板的完整数据快照，包括 `ActtemplateFull`（环节完整数据）、`CommentCategoryFull`（意见分类完整数据）和 `CommentGroupFull`（意见分组完整数据），支持一键复制流程模板。
