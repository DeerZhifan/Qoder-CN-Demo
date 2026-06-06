# CDP 工作流引擎（流程定义与管理）设计手册

> 对应使用手册：[cdp-module-workflow-process.md](../3-组件使用手册/cdp-module-workflow-process.md)

## 一、设计目标与背景

CDP 工作流引擎面向企业级审批场景，需满足以下核心需求：

1. **流程定义与实例分离**：模板变更不影响已运行的流程实例，保证生产环境稳定性。
2. **事件驱动的业务解耦**：流程引擎与业务系统通过事件接口交互，引擎不感知具体业务逻辑。
3. **多租户与多系统隔离**：同一套引擎可服务多个租户和多个接入系统，通过 `tenantId`、`systemId`、`companyId` 三级隔离。
4. **单体/微服务双模态部署**：所有 Business 接口同时标注 `@FeignClient`，在单体模式下作为本地 Bean 调用，在微服务模式下通过 OpenFeign 远程调用，零代码切换。

> 设计决策：选择自研轻量引擎而非集成 Activiti/Flowable，是为了在国产化数据库（DM、GaussDB、KingBase、GBase）上获得完全控制权，同时避免重量级 BPMN 引擎带来的学习成本和性能开销。

## 二、整体架构

工作流模块遵循 CDP 标准的五层子模块结构：

```
leatop-cdp-business-workflow/
  workflow-api          接口层：Business 接口、DTO/QO/VO、事件契约
  workflow-service      实现层：BusinessImpl、DAO/Mapper、PO、消息发送、定时任务
  workflow-controller   控制层：REST Controller
  workflow-boot-starter 单体 Starter
  workflow-cloud-starter 微服务 Starter
```

核心数据模型分为两大域：

- **模板域**（设计时）：`JobtemplatePo`（流程模板）-> `ActtemplatePo`（环节模板）-> `ActrelationPo`（环节路由）-> `ActvarPo`（环节变量）-> `EventPo` / `EventvarPo`（事件及事件变量）
- **实例域**（运行时）：`ProcessInstancePo`（流程实例）-> `ActivityPo`（活动实例）-> `TasksPo`（任务）-> `TaskcommentPo`（处理意见）

模板域与实例域通过 `jobTemplateId` 关联。流程启动时，引擎根据模板快照创建实例数据，后续模板修改不会影响已创建的实例。

> 设计决策：模板域和实例域使用独立的 PO 类和数据库表（`wf_` 前缀），而非共享实体，避免了模板变更对运行中实例的侧面影响。

## 三、核心设计模式

### 3.1 Template Method 模式 -- BaseFlowBusiness

`BaseFlowBusiness<T>` 是所有 CRUD 型 Business 接口的基类，定义了分页查询、新增、批量操作、更新、删除等通用操作骨架。具体业务接口（如 `BillBusiness`、`MonitorBusiness`、`ConfigBusiness`）继承该接口并扩展特有方法。

这一设计带来两个好处：

- Controller 层可统一处理通用 CRUD 请求，减少重复代码。
- Feign 客户端自动继承全部通用端点定义，微服务调用无需逐一声明。

### 3.2 Observer 模式 -- 事件驱动架构

引擎运行过程中通过三类事件接口通知外部系统：

| 接口 | 职责 | 触发时机 |
|------|------|----------|
| `IWorkflowCallback` | 事前/事后回调，可中断流程 | 环节开始、完成、退回等 |
| `ITaskEventListener` | 任务推送通知（不可中断） | 新任务生成、任务完成 |
| `IWorkflowPageEvent` | 页面级人员干预 | 展示审批人选择面板前 |

`IWorkflowCallback.execute()` 返回 `CallbackResult`，其中 `isSuccess=false` 时引擎可中止当前操作，实现"事前拦截"。`ITaskEventListener.onExecuteEvent()` 则为纯通知型，不影响流程推进。

事件监听器通过 `applicationContext-wfevent.xml` 注册为 Spring Bean，由 `EventConfiguration` 使用 `@ImportResource` 加载。这种 XML 配置方式使得业务系统无需修改 Java 代码即可调整事件行为。

> 设计决策：事件回调采用同步调用 + 异步消息发送的混合策略。`IWorkflowCallback` 同步执行以支持事务回滚，而 `MessageTaskEventListener` 中的消息发送通过 `AsyncTaskManager` 异步处理，避免阻塞审批主流程。

### 3.3 Strategy 模式 -- 消息发送策略

消息发送通过 `MessageSender` 接口抽象，`MessageConfiguration` 注册了 `defaultSmsMessageSender` 和 `defaultWxMessageSender` 两个实现。`MessageTaskEventListener` 在构造时接收具体 `MessageSender` 实例，运行时通过模板变量（`{@senderName}`、`{@title}` 等）动态替换消息内容。

业务系统可自定义 `MessageSender` 实现，支持短信、企业微信、钉钉等渠道扩展，无需修改引擎代码。

### 3.4 路由决策模式

环节间的流转通过 `ActrelationPo` 定义有向图关系，`ActvarPo` 定义路由条件变量。引擎在 `AnalysisBusiness.findNextAct()` 中计算下一环节时，评估变量表达式决定分支走向。`ActTemplatePath` 封装了从起始环节出发的人工环节列表和决策环节列表，支持顺序、分支、并行等多种路由模式。

`ActmutexPo` 定义互斥环节，保证同一流程实例中特定环节不会同时激活。`AssignbackactPo` 定义可退回的目标环节，限制退回范围。

## 四、关键类说明

### 接口层（workflow-api）

| 类 | 包路径 | 说明 |
|----|--------|------|
| `BaseFlowBusiness<T>` | `business` | 泛型 CRUD 基接口，定义分页/增删改查骨架 |
| `ProcessBusiness` | `business.api` | 流程生命周期操作：启动、子流程管理、状态查询 |
| `TaskBusiness` | `business.engine` | 任务处理：审批、撤回、退回、跳转、分发、指派 |
| `MonitorBusiness` | `business.engine` | 流程监控：任务列表、流程重置/回滚/终止、意见管理 |
| `FlowAdminBusiness` | `business.dupctrl` | 流程管理操作：特送、加签、减签、挂起、激活 |
| `FlowViewBusiness` | `business.dupctrl` | 流程视图：意见查询、流程图、流程跟踪、催办记录 |
| `AnalysisBusiness` | `business.engine` | 路由分析：查找下一环节、退回环节、预处理人 |
| `HomeBusiness` | `business.api` | 工作台门户：待办/已办/传阅/催办列表、任务统计 |
| `IWorkflowCallback` | `event` | 事前/事后回调接口 |
| `ITaskEventListener` | `event` | 任务事件推送接口 |
| `IWorkflowPageEvent` | `event` | 页面人员选择干预接口 |
| `IDigitalSign` | `event` | 处理意见数字签名扩展接口 |
| `ITasksurrogateListener` | `event` | 任务委托事件推送接口 |
| `TaskEventArgs` | `event` | 任务事件参数，携带流程实例、任务列表、操作类型 |
| `WorkflowCallbackEventArgs` | `event` | 回调事件参数，携带完整的上下文（实例、活动、模板、处理人、后续环节） |
| `WorkflowPageEventArgs` | `event` | 页面事件参数，携带环节信息和业务数据标识 |
| `CallbackResult` | `event` | 回调返回值，控制流程是否继续 |
| `SysConfigData` | `event` | 接入系统配置：事件回调地址、任务推送地址及授权 |

### 实现层（workflow-service）

| 类 | 包路径 | 说明 |
|----|--------|------|
| `EventConfiguration` | `configuration` | 通过 `@ImportResource` 加载 XML 事件配置和组织树 |
| `WorkflowConfiguration` | `configuration` | 注册 `AsyncTaskManager`（线程池前缀 WF-ADMIN） |
| `MessageConfiguration` | `configuration` | 注册短信/微信 `MessageSender` Bean |
| `AutoHistorySchedule` | `configuration` | 流程归档定时任务，按条件归档超期实例 |
| `UrgeScheduleConfiguration` | `configuration` | 任务自动催办定时任务 |
| `MessageTaskEventListener` | `message.impl` | `ITaskEventListener` 实现，模板化消息发送 |
| `MessageSender` | `message` | 消息发送策略接口 |
| `DefaultMessageSenderImpl` | `message.impl` | 默认消息发送实现 |
| `ActTemplatePath` | `bean` | 环节路径封装，包含人工环节和决策环节列表 |
| `ProcessInstancePo` | `model.instance` | 流程实例持久化对象 |
| `ActivityPo` | `model.instance` | 活动实例持久化对象 |
| `TasksPo` | `model.instance` | 任务持久化对象 |
| `JobtemplatePo` | `model.flow` | 流程模板持久化对象 |
| `ActtemplatePo` | `model.flow` | 环节模板持久化对象 |

## 五、扩展机制

### 5.1 自定义事件回调

实现 `IWorkflowCallback` 接口并在 `applicationContext-wfevent.xml` 中注册为 Bean。`execute()` 方法接收 `WorkflowCallbackEventArgs`，包含完整的流程上下文。返回 `CallbackResult.isSuccess=false` 可中止当前操作。

### 5.2 自定义消息通道

实现 `MessageSender` 接口，在 Spring 配置中替换或补充默认的 SMS/WeChat 发送器。`MessageTaskEventListener` 支持通过构造参数注入任意 `MessageSender`。

### 5.3 人员选择干预

实现 `IWorkflowPageEvent` 接口，在审批人展示前对候选人列表进行过滤或增补。典型场景：根据业务金额动态调整审批人层级。

### 5.4 数字签名扩展

实现 `IDigitalSign` 接口，在处理意见保存时注入签名逻辑（如 CA 证书签名）。

### 5.5 任务委托通知

实现 `ITasksurrogateListener` 接口，在委托创建/修改/删除时推送通知到外部系统。

### 5.6 定时任务扩展

`AutoHistorySchedule` 和 `UrgeScheduleConfiguration` 均通过 `@ConditionalOnProperty` 按需启用，cron 表达式通过配置文件控制。业务系统可参照此模式添加自定义定时任务。

## 六、模块协作

```
workflow-api (事件契约)
     |
     +--- workflow-service (引擎核心实现)
     |        |
     |        +--- model/flow/*Po  <-- 模板域 (wf_jobtemplate, wf_acttemplate ...)
     |        +--- model/instance/*Po <-- 实例域 (wf_processinstance, wf_activity, wf_tasks ...)
     |        +--- model/history/*Po  <-- 归档域 (wf_activity_history, wf_tasks_history)
     |        +--- message/*  <-- 消息发送
     |        +--- configuration/* <-- 自动配置
     |
     +--- workflow-controller (REST 端点)
     |
     +--- workflow-boot-starter   (单体部署自动装配)
     +--- workflow-cloud-starter  (微服务部署自动装配)
```

工作流引擎与其他 CDP 模块的协作关系：

- **cdp-business-system**：通过 `UserWfPo`、`RoleWfPo`、`OrgWfPo` 视图对接系统用户/角色/组织数据，`ot-tree*.xml` 配置组织树数据源。
- **cdp-business-message**：通过 `MessageSender` 接口对接消息模块，发送短信/微信通知。
- **cdp-base-cache**：流程模板数据可通过 CDP 两级缓存加速读取。
- **cdp-common-auth**：管理员操作（`SysmanageBusiness`）依赖 SA-Token 权限校验。
- **业务系统**：通过实现 `IWorkflowCallback`、`ITaskEventListener` 等事件接口与引擎解耦交互。

## 七、设计权衡与约束

1. **XML 配置 vs 注解配置**：事件监听器选用 XML 配置（`applicationContext-wfevent.xml`），虽然不如注解直观，但支持运维人员在不重新编译的情况下调整事件行为。这是面向运维便利性的权衡。

2. **同步回调 vs 异步回调**：`IWorkflowCallback` 采用同步调用以支持事务一致性，但 `rollbackExecute()` 方法已标记为 `@Deprecated`，表明事件操作与流程操作不在同一事务中，当前通过业务层补偿处理最终一致性。

3. **模板快照 vs 模板引用**：引擎在流程启动时不做模板深拷贝，而是通过 `jobTemplateId` 引用模板。虽然使用手册说明"模板修改后已运行的实例不受影响"，但这依赖于环节路由在创建活动实例时已固化到 `ActivityPo` 中。

4. **多数据库兼容**：Mapper XML 按数据库类型组织在 `mapper/`、`mapper/dm/`、`mapper/gauss/`、`mapper/kingbase/`、`mapper/gbase/` 目录下，部分查询（如 `OpiniontemplateDAO`、`TasksDAO`）针对不同数据库提供差异化 SQL 实现。

5. **Feign 双模态约束**：所有 Business 接口的 `@FeignClient` 注解使用 `${cdp.feign.workflow.url:}` 占位符，当 `url` 为空时走服务发现，非空时直连指定地址。单体模式下 Spring 将这些接口注册为本地 Bean 而非 Feign 代理。

6. **历史归档策略**：`AutoHistorySchedule` 通过 `BatchHistory.toHistory()` 将超期数据迁移到 `*_history` 表（`ActivityHistoryPo`、`TasksHistoryPo`），默认保留 6 个月。归档操作通过配置开关控制，避免对小型部署造成不必要开销。
