---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/08-module-workflow.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-workflow.md
---

# 工作流前端集成规范

## 适用场景

本规则适用于 CDP Web 前端框架中与工作流相关的所有开发任务，包括：
- 工作流前端集成（otworkflow 组件库安装与注册）
- 流程发起（业务单据提交并启动流程）
- 审批操作（通过、驳回、转办、撤回）
- 流程图展示（otchart 流程图组件）
- 工作流表单集成（业务表单与流程变量绑定）
- 流程状态枚举与展示
- WebSocket/STOMP 实时待办通知

## 前置依赖

- 后端工作流引擎（OT Workflow Engine）已部署并可访问
- `package.json` 中通过 `file:` 协议引用本地工作流库：
  - `msdp-common`、`msdp-common-ctrl`、`msdp-common-nui`、`otworkflow`
- 工作流插件已在 `main.ts` 中通过 `app.use(otworkflow)` 全局注册
- 工作流 CSS 已按正确顺序导入（Element Plus CSS 之后）
- 工作流路由已在 `src/_changeable/router.ts` 中聚合
- Vite 代理已配置工作流 API 前缀和 WebSocket 转发

## 配置要点

### 模块结构

```
src/workflow/
├── api/                  30+ 工作流 API（tasks.js / monitor.js / bill.js 等）
├── views/                50+ 工作流视图（bill/ chart/ config/ monitor/ 等）
├── router/               路由聚合（constantRoutes.js + asyncRoutes.js）
├── utils/                工作流工具函数
└── common-lib/           工作流依赖库
    ├── msdp-common/      MSDP 公共库
    ├── msdp-common-ctrl/ MSDP 控件库
    ├── msdp-common-nui/  MSDP NUI 组件
    ├── otworkflow/       OT 工作流核心引擎
    └── otchart/          流程图组件
```

### 插件注册顺序（main.ts）

```typescript
// 1. 导入插件
import otworkflow from "@/cdp-common-admin/components/otworkflow.js";
// 2. 导入 CSS（必须在 Element Plus CSS 之后）
import "msdp-common-nui/dist/msdp-common-nui.css";
import "msdp-common-ctrl/dist/msdp-common-ctrl.css";
import "otworkflow/dist/otworkflow.css";
// 3. 注册插件
app.use(otworkflow);
// 4. 动态注册 msdp-common-ctrl 组件
const componentsCtrl = import.meta.glob("@/msdp-common-ctrl/components/**/*.vue");
```

### 路由聚合

```typescript
// src/_changeable/router.ts
import workflowRouter from "@/workflow/router";
import otworkflow from "otworkflow";

export const constantRoutes = [
  ...workflowRouter.constantRoutes,
  ...otworkflow.workflowApiRoutes,
];
```

### 流程状态枚举

| 状态值 | 含义 | 说明 |
|--------|------|------|
| draft | 草稿 | 业务单据已保存，流程未发起 |
| running | 运行中 | 流程已发起，等待审批 |
| completed | 已完成 | 流程所有节点审批通过 |
| rejected | 已驳回 | 流程被驳回 |
| cancelled | 已撤回 | 发起人撤回流程 |
| suspended | 已挂起 | 流程被管理员挂起 |

## 代码模式

### 推荐写法

#### 发起流程

```typescript
async function submitAndStartFlow() {
  const { data } = await saveBusinessData(formData);
  await startProcess({
    processKey: "approval_flow_key",
    businessKey: data.id,
    variables: { amount: formData.amount },
  });
  ElMessage.success("提交成功，已发起审批流程");
}
```

#### 审批操作（通过/驳回/转办/撤回）

```typescript
import * as request from "@/cdp-common/utils/request";

// 通过
export function approveTask(data: { taskId: string; comment: string }) {
  return request.postJson("/workflow/task/complete", data);
}
// 驳回
export function rejectTask(data: { taskId: string; comment: string }) {
  return request.postJson("/workflow/task/reject", data);
}
// 转办
export function transferTask(data: { taskId: string; targetUserId: string }) {
  return request.postJson("/workflow/task/transfer", data);
}
// 撤回
export function withdrawProcess(processInstanceId: string) {
  return request.post(`/workflow/process/withdraw/${processInstanceId}`);
}
```

#### 工作流页面跳转

```typescript
const router = useRouter();
router.push("/workflow/todo");                          // 待办列表
router.push("/workflow/start");                         // 发起流程
router.push(`/workflow/detail/${processInstanceId}`);   // 流程详情
```

#### WebSocket/STOMP 实时通知

```typescript
import SockJS from "sockjs-client";
import Stomp from "stompjs";

const socket = new SockJS("/api/ws/workflow");
const stompClient = Stomp.over(socket);
stompClient.connect({}, () => {
  stompClient.subscribe("/user/queue/workflow/todo", (message) => {
    const data = JSON.parse(message.body);
    ElNotification({ title: "新待办任务", message: data.taskName, type: "info" });
  });
});
```

#### 查询待办列表

```typescript
export function getTodoList(params: any) {
  return request.postJson("/workflow/task/todoList", params);
}
```

### 禁止事项

1. **禁止绕过 otworkflow 插件直接操作工作流内部组件** — 必须通过 `app.use(otworkflow)` 注册后使用
2. **禁止在 Element Plus CSS 之前导入工作流 CSS** — 顺序错误会导致样式覆盖
3. **禁止手动注册 otworkflow 内置路由** — 通过 `otworkflow.workflowApiRoutes` 自动注册
4. **禁止在业务模块中硬编码流程定义 Key** — 应通过配置或后端接口获取
5. **禁止跳过业务数据保存直接发起流程** — 必须先保存业务单据再调用 startProcess
6. **禁止在组件中直接创建 WebSocket 连接而不管理断开** — 必须在 `onUnmounted` 中调用 disconnect
7. **禁止混用工作流 API 前缀与主系统 API 前缀** — 注意 Vite Proxy 配置区分
8. **禁止修改 `src/workflow/common-lib/` 下的库文件** — 这些是第三方依赖，通过升级而非修改来更新
