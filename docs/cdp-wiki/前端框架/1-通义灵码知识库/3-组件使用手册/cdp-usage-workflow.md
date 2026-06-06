# 如何集成工作流

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 工作流, workflow, OT, STOMP, WebSocket, 流程, 审批, otworkflow

---

## 概述

CDP Web 集成 OT 工作流引擎（`otworkflow` 组件库），提供流程设计、任务审批、流程监控等功能。工作流通过 WebSocket/STOMP 实现实时消息推送。相关代码位于 `src/workflow/` 和 `src/cdp-common-frame/workflow/`。

## 启用方式

工作流组件已在 `main.ts` 中全局注册：

```typescript
// src/main.ts
import otworkflow from "@/cdp-common-admin/components/otworkflow.js";
import "otworkflow/dist/otworkflow.css";
app.use(otworkflow);
```

工作流路由在 `src/_changeable/router.ts` 中自动聚合：

```typescript
import * as workflowRouter from "@/workflow/router";
import otworkflow from "@/cdp-common-admin/components/otworkflow.js";

export const constantRoutes = [
  ...workflowRouter.constantRoutes,
  ...otworkflow.workflowApiRoutes,  // 工作流内置路由
];
```

## 工作流页面路由

工作流提供 50+ 内置页面，通过 `otworkflow.workflowApiRoutes` 自动注册。业务模块可通过以下方式跳转到工作流页面：

```typescript
import { useRouter } from "vue-router";

const router = useRouter();

// 跳转到流程发起页
router.push("/workflow/start");

// 跳转到待办任务列表
router.push("/workflow/todo");

// 跳转到流程详情
router.push(`/workflow/detail/${processInstanceId}`);
```

## 调用工作流 API

工作流 API 通过 `src/cdp-common-frame/workflow/` 或 `src/workflow/` 目录下的接口模块调用：

```typescript
import * as request from "@/cdp-common/utils/request";

// 发起流程
export function startProcess(data: any) {
  return request.postJson("/workflow/process/start", data);
}

// 审批任务
export function completeTask(data: any) {
  return request.postJson("/workflow/task/complete", data);
}

// 查询待办列表
export function getTodoList(params: any) {
  return request.postJson("/workflow/task/todoList", params);
}
```

## WebSocket/STOMP 实时通知

工作流使用 STOMP over WebSocket 推送任务通知：

```typescript
import SockJS from "sockjs-client";
import Stomp from "stompjs";

// 建立连接
const socket = new SockJS("/api/ws/workflow");
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log("WebSocket 已连接", frame);

  // 订阅待办通知
  stompClient.subscribe("/user/queue/workflow/todo", (message) => {
    const data = JSON.parse(message.body);
    ElNotification({
      title: "新待办任务",
      message: data.taskName,
      type: "info",
    });
  });
});

// 断开连接
function disconnect() {
  if (stompClient) {
    stompClient.disconnect();
  }
}
```

## 工作流业务集成示例

在业务模块中集成工作流审批：

```vue
<script setup lang="ts">
// 提交业务单据并发起流程
async function submitAndStartFlow() {
  // 1. 保存业务数据
  const { data } = await saveOrder(formData);

  // 2. 发起工作流
  await startProcess({
    processKey: "order_approval",   // 流程定义 key
    businessKey: data.id,            // 业务单据 ID
    variables: {
      amount: formData.amount,       // 传递流程变量
    },
  });

  ElMessage.success("提交成功，已发起审批流程");
}
</script>
```

## 注意事项

> 注意：工作流组件库（`otworkflow`）通过 `app.use()` 全局安装，其内置路由和组件自动生效，无需手动注册。

> 注意：工作流的样式文件必须在 `main.ts` 中导入：`import "otworkflow/dist/otworkflow.css"` 以及 `import "msdp-common-nui/dist/msdp-common-nui.css"`。

> 注意：WebSocket 连接需要后端同步配置 STOMP Broker。开发环境通过 Vite 代理转发 WebSocket 请求。

> 注意：工作流模块相对独立，升级 `otworkflow` 版本后需回归测试流程功能。
