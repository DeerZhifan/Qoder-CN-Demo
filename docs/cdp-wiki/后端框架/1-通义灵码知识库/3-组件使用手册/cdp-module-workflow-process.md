# 如何使用 CDP 工作流 — 流程定义与管理

## 概述

工作流组件（`leatop-cdp-business-workflow`）支持自定义、执行和管理工作流程，提供流程定义、环节配置、事件驱动等能力。本文档介绍流程定义和管理相关功能。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-workflow-boot-starter</artifactId>
</dependency>
```

需配置工作流相关数据库表（`wf_*` 开头）和视图（`v_workflow_` 开头），以及 `applicationContext-wfevent.xml` 事件配置文件。

## 核心功能

### 流程定义

- **流程模板管理**：定义流程的基本信息、环节、路由和事件
- **环节模板（Acttemplate）**：定义流程中的每个审批环节，包括审批人规则、时限等
- **环节变量（Actvar）**：定义环节中的变量，用于动态控制流程行为
- **事件配置（Event）**：定义流程和环节级别的事件（提交、退回、完成等）

### 流程操作

- **发起流程（BaseFlow）**：业务单据关联流程，启动审批
- **流程视图（FlowView）**：查看流程运行状态和审批历史
- **流程管理（FlowAdmin）**：管理员干预流程（暂停、恢复、终止）

### 单据管理

- **Bill**：业务单据与工作流的关联管理
- **Billurl**：单据页面 URL 配置，支持审批页面跳转

## 流程集成方式

业务系统通过实现工作流事件接口与流程引擎交互：

```xml
<!-- applicationContext-wfevent.xml 事件监听配置 -->
<bean id="messageTaskEventListener"
      class="com.leatop.cdp.workflow.message.impl.MessageTaskEventListener">
    <constructor-arg index="0">
        <value>OA消息{@senderName}于{@now}发送了"{@title}"审批流中
               【{@actTemplateName}】环节的任务给您，请及时处理。</value>
    </constructor-arg>
    <constructor-arg ref="defaultSmsMessageSender"/>
</bean>
```

支持的消息变量：`{@senderName}`（发送人）、`{@receiverName}`（接收人）、`{@now}`（当前时间）、`{@title}`（标题）、`{@actTemplateName}`（环节名称）、`{@urgentLevel}`（紧急程度）。

## 注意事项

> 注意：工作流组件的数据库表以 `wf_` 为前缀，视图以 `v_workflow_` 为前缀，需通过 Flyway 或手动创建。

> 注意：流程事件配置通过 Spring XML 配置文件定义，支持短信、企业微信等多种消息发送方式。
