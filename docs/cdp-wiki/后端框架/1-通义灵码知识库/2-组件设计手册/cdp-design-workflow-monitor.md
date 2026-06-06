# CDP 工作流监控与统计 设计手册

> 对应使用手册：[cdp-module-workflow-monitor.md](../3-组件使用手册/cdp-module-workflow-monitor.md)

## 一、设计目标与背景

流程监控面向系统管理员和流程管理员，提供对运行中流程的全局可见性和干预能力。设计目标：

- 实时查看所有流程实例的运行状态，快速定位阻塞或超时的流程。
- 提供管理员级别的流程干预操作（终止、回滚、重置、挂起/激活）。
- 提供任务限时统计和效率分析，支撑流程优化决策。
- 监控操作受权限控制，通过 `SysmanageBusiness` 管理管理员身份。

> 设计决策：监控与任务操作分离为独立的 Business 接口。`MonitorBusiness` 继承 `BaseFlowBusiness<TasksDto>` 获得通用查询能力的同时，扩展了管理员专属操作。普通用户通过 `HomeBusiness` 查看自己的任务，管理员通过 `MonitorBusiness` 查看全局任务。

## 二、整体架构

监控功能分布在三个层级：

```
[监控管理层] -- 管理员操作
  MonitorBusiness    -- 流程实例级监控与干预
  FlowAdminBusiness  -- 流程管理操作（特送、加签、挂起、激活）
  SysmanageBusiness  -- 管理员权限配置

[统计分析层] -- 数据聚合
  ReportBusiness     -- 任务限时统计报表
  AnalysisBusiness   -- 流程路由分析（下一环节、退回环节）
  ChartController    -- 分析图表 REST 端点

[视图查询层] -- 只读查询
  FlowViewBusiness   -- 流程跟踪：意见、流程图、催办记录、传阅记录
  HomeBusiness       -- 催办统计（statUrgeList）、督办统计（statMonitorList）
```

监控数据来源分为实时查询和归档查询。实时数据从 `wf_tasks`、`wf_activity`、`wf_processinstance` 表查询；归档数据从 `ActivityHistoryPo`、`TasksHistoryPo` 对应的历史表查询。`AutoHistorySchedule` 定期将超期数据迁移到历史表，保证实时查询的性能。

## 三、关键类说明

| 类 | 说明 |
|----|------|
| `MonitorBusiness` | 核心监控接口。`todoTaskList()` 管理员级待办查询；`processInstPage()` 流程实例分页；`deleteFlow()` 删除流程；`processReset()` 流程重置；`processInstRollback()` 流程回滚；`processeTermination()` 流程终止；`taskTransfer()` 任务移交 |
| `FlowAdminBusiness` | 流程干预接口。`processSuspend()` 挂起流程；`processActivate()` 激活流程；`specialTransfer()` 特送到指定环节和人员；`addSign()` 加签；`findElectionNextAct()` 补选环节 |
| `ReportBusiness` | 统计报表接口。`list()` 接收 `TaskStatisticsQueryVo` 返回 `TaskStatisticsDto` 分页数据，提供任务限时统计 |
| `FlowViewBusiness` | 流程视图接口。`getTaskComments()` 获取全部意见；`findActTempAndActors()` 获取流程跟踪数据；`getCharByTempId()` 获取流程图；`findFlowUrges()` 催办记录；`findFlowCanreads()` 传阅记录 |
| `SysmanageBusiness` | 管理员配置接口，继承 `BaseFlowBusiness<SysmanageDto>`，管理工作流系统管理员 |
| `MonitorController` | 监控 REST 端点（`controller.admin` 包） |
| `ReportController` | 报表 REST 端点（`controller.admin` 包） |
| `ChartController` | 图表 REST 端点 |
| `TaskStatisticsDto` | 任务统计数据传输对象 |
| `TaskStatisticsQueryVo` | 任务统计查询参数对象 |
| `AutoHistorySchedule` | 流程归档定时任务，将超过 `beyondMonth`（默认 6 个月）的数据迁移到历史表 |
| `ActivityHistoryBusiness` / `TasksHistoryBusiness` | 历史数据查询接口（`business.history` 包） |

## 四、扩展机制

1. **管理员权限扩展**：`SysmanageBusiness` 管理工作流管理员列表，与 CDP 系统权限模块（SA-Token RBAC）配合使用。业务系统可通过扩展 `SysmanagePo` 配置不同级别的管理权限。

2. **归档策略定制**：`AutoHistorySchedule` 通过 `workflow.schedule.archive.status`、`workflow.schedule.archive.cron`、`workflow.schedule.archive.beyondMonth` 三个配置项控制是否启用、执行频率和归档阈值。底层 `BatchHistory` 服务可被替换为自定义归档实现。

3. **意见管理扩展**：`MonitorBusiness` 提供 `saveCommentAndLog()`、`saveCommentHandlerAndLog()` 用于管理员修改处理意见，修改记录通过 `TaskcommnetLogBusiness` 持久化，保证审计可追溯。`hideShowComment()` 可隐藏/显示特定意见，`deleteComment()` 可删除意见。

4. **自定义统计维度**：`ReportBusiness` 当前提供任务限时统计。业务系统可参照 `ReportBusinessImpl` 的实现模式，基于 `TaskStatisticsDao` 扩展自定义统计维度（按部门、按流程类型、按时间段等）。
