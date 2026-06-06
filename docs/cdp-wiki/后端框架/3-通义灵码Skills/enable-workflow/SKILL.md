# 启用 CDP 工作流引擎

## 描述

在已有 CDP 项目中启用工作流引擎组件（`leatop-cdp-business-workflow`），提供流程定义、环节配置、任务审批、单据管理、流程监控等完整的工作流能力。引擎采用自研轻量方案，支持国产化数据库，支持单体/微服务双模态部署。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体部署 或 `cloud` 微服务部署，默认 `boot`）
3. **是否需要消息通知**（短信/微信等，默认否）
4. **是否启用流程归档**（定时将超期数据迁移到历史表，默认否）
5. **是否启用自动催办**（对超时任务自动发送催办，默认否）

---

## 步骤 1：添加 Maven 依赖

> 根据部署模式引入对应的 Starter。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加（二选一）：

**单体部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-workflow-boot-starter</artifactId>
</dependency>
```

**微服务部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-workflow-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：初始化数据库

> 工作流引擎需要 `wf_*` 前缀的数据库表和 `v_workflow_*` 前缀的视图。通过 Flyway 迁移脚本或手动执行 SQL 创建。

确保以下数据库对象已创建：

- 模板域表：`wf_jobtemplate`、`wf_acttemplate`、`wf_actrelation`、`wf_actvar`、`wf_event`、`wf_eventvar` 等
- 实例域表：`wf_processinstance`、`wf_activity`、`wf_tasks`、`wf_taskcomment` 等
- 归档域表：`wf_activity_history`、`wf_tasks_history` 等
- 单据相关表：`wf_bill`、`wf_billurl`、`wf_billurl_formvar`、`wf_bill_company` 等
- 视图：`v_workflow_*` 前缀的视图用于待办/已办等查询

## 步骤 3：配置事件监听器

> 事件配置通过 Spring XML 文件定义，由 `EventConfiguration` 使用 `@ImportResource` 自动加载。在 classpath 下创建 `applicationContext-wfevent.xml`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 消息通知监听器（按需配置） -->
    <bean id="messageTaskEventListener"
          class="com.leatop.cdp.workflow.message.impl.MessageTaskEventListener">
        <constructor-arg index="0">
            <value>OA消息{@senderName}于{@now}发送了"{@title}"审批流中
                   【{@actTemplateName}】环节的任务给您，请及时处理。</value>
        </constructor-arg>
        <constructor-arg ref="defaultSmsMessageSender"/>
    </bean>

</beans>
```

支持的消息变量：`{@senderName}`（发送人）、`{@receiverName}`（接收人）、`{@now}`（当前时间）、`{@title}`（标题）、`{@actTemplateName}`（环节名称）、`{@urgentLevel}`（紧急程度）。

## 步骤 4：配置 YAML（可选项）

> 如需启用流程归档或自动催办，在 `application.yaml` 或对应 profile 中添加配置。

```yaml
workflow:
  schedule:
    archive:
      status: true                          # 是否启用流程归档
      cron: "0 0 2 * * ?"                   # 归档执行频率（每天凌晨2点）
      beyondMonth: 6                        # 超过多少个月的数据归档
    urge:
      status: true                          # 是否启用自动催办
      cron: "0 0 9 * * MON-FRI"             # 催办频率（工作日上午9点）
```

## 步骤 5：实现业务事件回调（按需）

> 业务系统通过实现事件接口与流程引擎解耦交互。`IWorkflowCallback` 支持事前拦截（返回失败可中止流程），`ITaskEventListener` 为纯通知型。

**事件回调示例：**

```java
public class MyWorkflowCallback implements IWorkflowCallback {

    @Override
    public CallbackResult execute(WorkflowCallbackEventArgs args) {
        String buDataId = args.getBuDataId();
        // 根据业务需要处理，如更新单据状态
        return CallbackResult.success();
    }
}
```

在 `applicationContext-wfevent.xml` 中注册：

```xml
<bean id="myWorkflowCallback" class="com.example.MyWorkflowCallback"/>
```

**任务事件监听示例：**

```java
public class MyTaskEventListener implements ITaskEventListener {

    @Override
    public void onExecuteEvent(TaskEventArgs args) {
        // 推送任务通知到外部系统
    }
}
```

## 步骤 6：配置单据类型

> 在工作流管理后台配置单据类型（Bill），关联审批页面 URL 和表单变量映射。流程启动时通过 `billcode` + `buDataId` 建立业务关联。

业务发起流程代码示例：

```java
@Autowired
private ProcessBusiness processBusiness;

public void startApproval(String billCode, String businessDataId) {
    StartExtendArgsDto args = new StartExtendArgsDto();
    args.setBillcode(billCode);
    args.setBuDataId(businessDataId);
    processBusiness.startFlow(args);
}
```

## 步骤 7：验证

启动应用，检查以下内容：

1. 控制台无工作流相关初始化异常
2. `wf_*` 数据库表可正常访问
3. 通过 `ProcessBusiness.startFlow()` 可成功发起流程
4. 通过 `HomeBusiness.todoList()` 可查询待办任务
5. 通过 `TaskBusiness.transactTask()` 可完成审批操作

---

## 完成后提醒

1. 流程模板修改后仅对新发起的流程生效，已运行实例不受影响
2. 事件监听器通过 XML 配置注册，支持短信（`defaultSmsMessageSender`）和微信（`defaultWxMessageSender`）两种内置消息通道
3. 自定义消息通道需实现 `MessageSender` 接口并在 Spring 配置中注册
4. 监控和管理员操作（终止、回滚、重置）需通过 `SysmanageBusiness` 配置管理员权限
5. 不要直接操作 `wf_*` 数据库表，必须通过 Business 接口操作
6. 不要同时引入 `workflow-boot-starter` 和 `workflow-cloud-starter`
