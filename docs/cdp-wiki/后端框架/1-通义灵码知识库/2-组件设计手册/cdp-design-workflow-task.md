# CDP 工作流任务管理 设计手册

> 对应使用手册：[cdp-module-workflow-task.md](../3-组件使用手册/cdp-module-workflow-task.md)

## 一、设计目标与背景

任务管理是工作流引擎面向终端用户的核心交互层，负责将流程引擎的运行状态转化为用户可操作的待办/已办任务。设计目标包括：

- 提供统一的任务操作入口，屏蔽底层流程引擎的复杂路由逻辑。
- 支持丰富的任务操作语义：审批提交、退回、撤回、转办、加签、拒签、跳转、分发、指派。
- 支持任务委托（代办）机制，处理人员请假或岗位变动场景。
- 支持催办、督办和传阅等协同操作。

## 二、整体架构

任务管理横跨 `engine` 和 `api` 两个子包，按职责分为三个层次：

```
[用户交互层]
  HomeBusiness         -- 工作台：待办/已办/传阅/催办/督办/反馈列表
  TaskController       -- 任务处理 REST 端点
  HomeController       -- 工作台 REST 端点

[任务处理层]
  TaskBusiness         -- 核心任务操作（提交、退回、撤回、跳转、分发）
  TransactTaskBusiness -- 任务办理底层实现
  DistributeTaskBusiness -- 任务分发逻辑
  BackTaskBusiness     -- 退回处理逻辑
  AssignBusiness       -- 任务指派

[辅助功能层]
  TasksurrogateBusiness    -- 任务委托管理
  UrgingBusiness           -- 催办操作
  CirculatBusiness         -- 传阅操作
  OpinionBusiness          -- 审批意见管理
  TaskcommentBusiness      -- 处理意见 CRUD
  TaskcommnetLogBusiness   -- 意见修改日志
```

待办/已办查询通过 `HomeBusiness` 统一入口，接收 `TodoTaskListRequest` 参数对象，内部通过数据库视图（`v_workflow_*` 前缀）实现多表关联查询，避免在 Java 层做复杂的数据拼装。

> 设计决策：将查询和操作分离到不同 Business 接口（`HomeBusiness` 负责查询，`TaskBusiness` 负责操作），遵循 CQRS 思想。查询走视图优化性能，操作走事务保证一致性。

## 三、关键类说明

| 类 | 说明 |
|----|------|
| `TaskBusiness` | 任务操作核心接口。`transactTask()` 接收 `TransactTaskVo` 完成审批，`backTask()` 处理退回，`recallTask()` 撤回已发任务，`jumpTask()` 跳转到指定环节，`distributeTask()` 分发任务 |
| `HomeBusiness` | 工作台查询接口。`todoList()` 查询待办/已办，`toreadList()` 查询传阅，`urgeList()`/`monitorList()` 查询催办/督办，`getTodoCount()`/`getTodoCounts()` 获取任务统计 |
| `TasksurrogateBusiness` | 任务委托接口。`saveData()` 保存委托配置，`getTasksurrogateList()` 查询委托列表，`cancelTableData()` 撤销委托 |
| `UrgingBusiness` | 催办接口，触发催办消息发送 |
| `CirculatBusiness` | 传阅接口，将流程信息传阅给非审批人 |
| `OpinionBusiness` | 审批意见管理，支持意见暂存和查询 |
| `FlowAdminBusiness` | 管理员任务干预：特送（`specialTransfer`）、加签（`addSign`）、挂起（`processSuspend`）、激活（`processActivate`） |
| `UrgeScheduleConfiguration` | 自动催办定时任务，通过 `@ConditionalOnProperty` 按需启用 |
| `AutoSignSchedule` | 自动签收定时任务 |
| `TasksPo` | 任务持久化对象（`model.instance`），映射 `wf_tasks` 表 |
| `TasksurrogatePo` | 任务委托持久化对象（`model.base`），映射 `wf_tasksurrogate` 表 |
| `TaskcommentPo` | 处理意见持久化对象（`model.instance`） |
| `TaskEventArgs` | 任务事件参数，携带 `taskEventType`（新建/完成等）和 `operateType`（提交/退回等） |

## 四、扩展机制

1. **自定义任务通知**：实现 `ITaskEventListener` 接口，在 `onExecuteEvent()` 中处理任务创建/完成事件。`TaskEventArgs` 提供流程实例和任务列表上下文，可据此推送到企业微信、钉钉或自定义系统。

2. **自定义委托通知**：实现 `ITasksurrogateListener` 接口，在委托新增（`taskEventType=0`）、修改（`1`）、删除（`2`）时接收回调，推送委托变更信息。

3. **自动催办策略**：`UrgeScheduleConfiguration` 通过 `workflow.schedule.urge.status` 开关启用，cron 表达式可配置。底层调用 `UrgeTasks.autoUrge()` 对超时任务自动发送催办消息。

4. **数字签名集成**：实现 `IDigitalSign` 接口的 `doTaskComment()` 方法，在处理意见保存时注入 CA 签名逻辑，适用于政务审批等需电子签章的场景。

5. **批量操作**：`HomeBusiness.batchSignTasks()` 和 `delayTasks()` 支持批量签收和批量缓办，适用于大量待办积压的处理场景。
