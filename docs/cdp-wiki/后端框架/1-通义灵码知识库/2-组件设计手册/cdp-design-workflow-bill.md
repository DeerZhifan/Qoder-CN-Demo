# CDP 工作流单据管理 设计手册

> 对应使用手册：[cdp-module-workflow-bill.md](../3-组件使用手册/cdp-module-workflow-bill.md)

## 一、设计目标与背景

工作流引擎本身只关心流程流转逻辑，不理解业务数据。单据管理模块的职责是建立业务数据与流程引擎之间的桥梁，解决三个核心问题：

- 业务数据如何关联到流程实例（一条请假单对应一个审批流程）。
- 审批页面如何定位和跳转（审批人点击待办后看到哪个页面）。
- 业务字段如何参与流程路由决策（如金额大于 10 万走总经理审批）。

> 设计决策：单据管理采用"间接引用"模式而非"嵌入式"模式。流程实例通过 `buDataId`（业务数据 ID）和 `billId`（单据类型 ID）引用业务数据，而非将业务字段存入流程表。这保证了引擎与业务的彻底解耦。

## 二、整体架构

单据管理由四个核心实体组成，形成层次化的配置结构：

```
Bill（单据类型）
  |-- 定义一种业务单据（如"请假申请"、"采购订单"）
  |-- 关联 systemId 实现多系统隔离
  |
  +-- BillCompany（单据公司配置）
  |     |-- 控制哪些公司/组织可使用该单据类型
  |
  +-- Billurl（单据 URL）
  |     |-- 定义审批页面地址，区分 PC/移动端、查看/编辑模式
  |     |-- 通过 actTemplateId 关联环节，不同环节可展示不同页面
  |     |
  |     +-- BillurlFormvar（表单变量映射）
  |           |-- 将业务字段映射为流程变量
  |           |-- 流程路由条件可引用这些变量
```

流程启动时，`ProcessBusiness.startFlow()` 接收 `StartExtendArgsDto`（包含 `billcode` 和 `buDataId`），引擎据此创建 `ProcessInstancePo` 并记录业务关联。审批人查看待办时，`HomeBusiness.getEditUrl()` 根据 `billcode` 和 `buDataId` 查找对应的 `Billurl` 配置，拼接出审批页面 URL。

## 三、关键类说明

| 类 | 说明 |
|----|------|
| `BillBusiness` | 单据类型管理接口，继承 `BaseFlowBusiness<BillDto>`。`getBillList()` 按系统 ID 查询可用单据，`validateAccount()` 校验单据是否存在 |
| `BillurlBusiness` | 单据 URL 管理接口。`actTemplateList()` 获取指定环节的驱动页配置，`findByActTemplate()` 按环节查询 URL 列表 |
| `BillurlFormvarBusiness` | 表单变量映射管理接口，继承 `BaseFlowBusiness<BillurlFormvarDto>`，提供标准 CRUD |
| `BillCompanyBusiness` | 单据公司授权管理接口 |
| `BillPo` / `BillurlPo` / `BillurlFormvarPo` / `BillCompanyPo` | 对应的持久化对象，位于 `model.base` 包 |
| `BillDto` / `BillurlDto` / `BillurlFormvarDto` / `BillCompanyDto` | 对应的数据传输对象，位于 `dto.base` 包 |
| `ProcessBusiness` | 流程启动时通过 `billcode` + `buDataId` 建立业务关联 |
| `HomeBusiness` | `getEditUrl()` 和 `getProcessDoUrl()` 根据单据配置定位审批页面 |

## 四、扩展机制

1. **多系统单据隔离**：`BillBusiness.getBillList()` 接收 `systemId` 参数，不同接入系统维护独立的单据类型集合。结合 `BillCompanyBusiness` 实现公司级别的细粒度授权。

2. **环节级页面定制**：`Billurl` 通过 `actTemplateId` 关联具体审批环节，同一单据在不同环节可展示不同页面（如发起环节展示可编辑表单，审批环节展示只读详情）。

3. **表单变量驱动路由**：`BillurlFormvar` 将业务字段映射为流程变量后，`ActvarPo` 中的路由条件表达式可引用这些变量值，实现基于业务数据的动态分支路由。

4. **多端适配**：`HomeBusiness.getEditUrl()` 支持 `fromType` 参数区分 PC 端和移动端，`Billurl` 可为同一环节配置不同终端的页面地址。`TaskBusiness.forbidMobile()` 可控制特定任务是否在移动端显示。
