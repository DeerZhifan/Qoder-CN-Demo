---
trigger: when_referenced
knowledge_source:
  - cdp-design-workflow-bill
  - cdp-design-workflow-config
  - cdp-design-workflow-monitor
  - cdp-design-workflow-process
  - cdp-design-workflow-task
  - cdp-module-workflow-bill
  - cdp-module-workflow-config
  - cdp-module-workflow-monitor
  - cdp-module-workflow-process
  - cdp-module-workflow-task
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-workflow-boot-starter` 或 `leatop-cdp-business-workflow-cloud-starter` 依赖
- 使用 `ProcessBusiness`、`TaskBusiness`、`HomeBusiness`、`MonitorBusiness` 等工作流接口
- 实现 `IWorkflowCallback`、`ITaskEventListener`、`IWorkflowPageEvent` 事件接口
- 配置 `applicationContext-wfevent.xml` 事件监听器
- 操作 `wf_*` 前缀的数据库表或 `v_workflow_*` 前缀的视图

---

## 前置依赖

1. Maven 依赖（单体/微服务二选一）：

```xml
<!-- 单体部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-workflow-boot-starter</artifactId>
</dependency>

<!-- 微服务部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-workflow-cloud-starter</artifactId>
</dependency>
```

2. 数据库表（`wf_*` 前缀）和视图（`v_workflow_*` 前缀）需通过 Flyway 或手动创建。

3. 事件配置文件 `applicationContext-wfevent.xml` 需放置在 classpath 下，由 `EventConfiguration` 通过 `@ImportResource` 加载。

---

## 配置要点

- 数据库表前缀为 `wf_`，视图前缀为 `v_workflow_`，不可更改。
- 流程模板修改后仅对新发起的流程生效，已运行实例不受影响。
- 事件监听器通过 XML 配置注册为 Spring Bean，支持运维人员在不重新编译的情况下调整行为。
- 归档配置项：`workflow.schedule.archive.status`（开关）、`workflow.schedule.archive.cron`（频率）、`workflow.schedule.archive.beyondMonth`（归档阈值，默认 6 个月）。
- 自动催办配置项：`workflow.schedule.urge.status`（开关）、对应 cron 表达式。
- Feign 双模态：Business 接口的 `@FeignClient` 使用 `${cdp.feign.workflow.url:}` 占位符，url 为空走服务发现，非空走直连。

---

## 代码模式

### 推荐写法

**事件回调：实现 IWorkflowCallback**

```java
public class MyWorkflowCallback implements IWorkflowCallback {

    @Override
    public CallbackResult execute(WorkflowCallbackEventArgs args) {
        // 根据流程上下文执行业务逻辑
        String billId = args.getBuDataId();
        // 返回 isSuccess=false 可中止当前操作
        return CallbackResult.success();
    }
}
```

**任务事件监听：实现 ITaskEventListener**

```java
public class MyTaskEventListener implements ITaskEventListener {

    @Override
    public void onExecuteEvent(TaskEventArgs args) {
        // 纯通知型，不影响流程推进
        // 可推送到企业微信、钉钉等
    }
}
```

**消息模板配置（applicationContext-wfevent.xml）**

```xml
<bean id="messageTaskEventListener"
      class="com.leatop.cdp.workflow.message.impl.MessageTaskEventListener">
    <constructor-arg index="0">
        <value>OA消息{@senderName}于{@now}发送了"{@title}"审批流中
               【{@actTemplateName}】环节的任务给您，请及时处理。</value>
    </constructor-arg>
    <constructor-arg ref="defaultSmsMessageSender"/>
</bean>
```

**流程发起：通过 ProcessBusiness 启动**

```java
// 传入 billcode 和 buDataId 建立业务关联
StartExtendArgsDto args = new StartExtendArgsDto();
args.setBillcode("LEAVE_APPLY");
args.setBuDataId(leaveRecordId);
processBusiness.startFlow(args);
```

### 禁止事项

- **禁止将业务字段直接存入流程表** -- 引擎采用"间接引用"模式，通过 `buDataId` + `billId` 引用业务数据，保持引擎与业务彻底解耦
- **禁止直接操作 `wf_*` 数据库表** -- 必须通过 Business 接口操作，绕过接口会破坏数据一致性和事件触发链
- **禁止在 `IWorkflowCallback.execute()` 中静默吞掉异常** -- 返回 `CallbackResult.isSuccess=false` 才能正确中止流程，静默捕获会导致流程在错误状态下继续推进
- **禁止混用单体和微服务 Starter** -- 同一个应用只能引入 `workflow-boot-starter` 或 `workflow-cloud-starter` 之一
- **禁止修改运行中实例对应的模板数据并期望立即生效** -- 模板变更仅对新发起的流程生效
- **禁止绕过 `SysmanageBusiness` 执行管理员级操作** -- 流程终止、回滚、重置等操作需管理员权限校验
- **禁止在 `ITaskEventListener.onExecuteEvent()` 中执行耗时操作** -- 该回调为同步通知型，耗时操作应异步化处理
- **禁止手动构造 `MessageSender`** -- 通过 `MessageConfiguration` 注册的 Bean 注入使用，自定义通道需实现 `MessageSender` 接口并注册为 Spring Bean
