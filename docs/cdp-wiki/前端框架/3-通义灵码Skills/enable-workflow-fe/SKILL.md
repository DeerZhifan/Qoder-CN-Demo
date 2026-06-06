---
type: enable
description: 为 CDP 前端项目启用工作流前端功能
rule: cdp-rule-workflow-fe.md
---

# 启用工作流前端功能

## 前提条件

在执行本技能之前，请确认以下条件：

- 后端工作流引擎（OT Workflow Engine）已部署并正常运行
- 工作流相关 API 接口可用（`/workflow/**` 路径）
- CDP Web 前端项目已正常运行（`pnpm run dev` 可启动）
- WebSocket/STOMP Broker 已在后端配置（如需实时通知）

## 执行步骤

### 第 1 步：安装工作流依赖

确认 `package.json` 中包含工作流本地库引用：

```json
{
  "dependencies": {
    "msdp-common": "file:src/workflow/common-lib/msdp-common",
    "msdp-common-ctrl": "file:src/workflow/common-lib/msdp-common-ctrl",
    "msdp-common-nui": "file:src/workflow/common-lib/msdp-common-nui",
    "otworkflow": "file:src/workflow/common-lib/otworkflow"
  }
}
```

执行安装：

```bash
pnpm install
```

> 注意：工作流库通过 `file:` 协议引用 `src/workflow/common-lib/` 下的本地目录，`pnpm install` 后会创建软链接。

### 第 2 步：配置工作流 API 与插件注册

在 `src/main.ts` 中添加工作流插件注册：

```typescript
// 1. 导入工作流插件
import otworkflow from "@/cdp-common-admin/components/otworkflow.js";

// 2. 导入工作流 CSS（必须在 Element Plus CSS 之后）
import "msdp-common-nui/dist/msdp-common-nui.css";
import "msdp-common-ctrl/dist/msdp-common-ctrl.css";
import "otworkflow/dist/otworkflow.css";

// 3. 注册插件
app.use(otworkflow);

// 4. 动态注册 msdp-common-ctrl 组件
const componentsCtrl = import.meta.glob("@/msdp-common-ctrl/components/**/*.vue");
Object.keys(componentsCtrl).forEach((key) => {
  componentsCtrl[key]().then((component) => {
    const componentName = component.default.name;
    if (!componentName) return;
    app.component(componentName, component.default || component);
  });
});
```

配置 Vite 代理（`vite.config.ts`），确保工作流 API 和 WebSocket 请求被正确转发：

```typescript
proxy: {
  "/workflow-api": {
    target: "http://后端地址:端口",
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/workflow-api/, ""),
  },
  "/api/ws": {
    target: "http://后端地址:端口",
    ws: true,
    changeOrigin: true,
  },
}
```

### 第 3 步：创建审批操作组件

创建通用审批操作组件 `src/workflow/components/ApprovalActions.vue`：

```vue
<script setup lang="ts">
import * as request from "@/cdp-common/utils/request";

const props = defineProps<{
  taskId: string;
  processInstanceId: string;
}>();

const emit = defineEmits<{
  (e: "success"): void;
}>();

const comment = ref("");

// 通过
async function handleApprove() {
  await request.postJson("/workflow/task/complete", {
    taskId: props.taskId,
    comment: comment.value,
  });
  ElMessage.success("审批通过");
  emit("success");
}

// 驳回
async function handleReject() {
  if (!comment.value) {
    ElMessage.warning("驳回时必须填写审批意见");
    return;
  }
  await request.postJson("/workflow/task/reject", {
    taskId: props.taskId,
    comment: comment.value,
  });
  ElMessage.success("已驳回");
  emit("success");
}

// 转办
const transferDialogVisible = ref(false);
const targetUserId = ref("");

async function handleTransfer() {
  await request.postJson("/workflow/task/transfer", {
    taskId: props.taskId,
    targetUserId: targetUserId.value,
  });
  ElMessage.success("已转办");
  transferDialogVisible.value = false;
  emit("success");
}

// 撤回
async function handleWithdraw() {
  await ElMessageBox.confirm("确认撤回该流程？", "提示", { type: "warning" });
  await request.post(`/workflow/process/withdraw/${props.processInstanceId}`);
  ElMessage.success("已撤回");
  emit("success");
}
</script>

<template>
  <div class="approval-actions">
    <el-input
      v-model="comment"
      type="textarea"
      placeholder="请输入审批意见"
      :rows="3"
      class="mb-4"
    />
    <div class="action-buttons">
      <el-button type="primary" @click="handleApprove">通过</el-button>
      <el-button type="danger" @click="handleReject">驳回</el-button>
      <el-button type="warning" @click="transferDialogVisible = true">转办</el-button>
      <el-button @click="handleWithdraw">撤回</el-button>
    </div>
  </div>
</template>
```

### 第 4 步：集成流程图展示与路由注册

在 `src/_changeable/router.ts` 中聚合工作流路由：

```typescript
import workflowRouter from "@/workflow/router";
import otworkflow from "otworkflow";

export const constantRoutes = [
  ...cdpCommonFrameRoutes.constantRoutes,
  ...workflowRouter.constantRoutes,
  ...otworkflow.workflowApiRoutes,
];
```

在业务页面中嵌入流程图展示：

```vue
<script setup lang="ts">
const router = useRouter();

// 查看流程图
function viewFlowChart(processInstanceId: string) {
  router.push(`/workflow/detail/${processInstanceId}`);
}

// 跳转到待办列表
function goToTodoList() {
  router.push("/workflow/todo");
}
</script>
```

### 第 5 步：验证

执行以下验证步骤确保工作流功能正常：

```bash
# 1. 重新安装依赖
pnpm install

# 2. 启动开发服务器
pnpm run dev

# 3. 运行代码检查
pnpm run lint:eslint
```

验证清单：
- [ ] 开发服务器启动无报错
- [ ] 访问 `/workflow/todo` 可正常显示待办列表
- [ ] 工作流组件样式正确（无 CSS 覆盖问题）
- [ ] 流程发起接口可正常调用
- [ ] 审批操作（通过/驳回）可正常执行
- [ ] 流程图页面可正常渲染
- [ ] WebSocket 实时通知可正常接收（如已配置）

## 完成后提醒

- 工作流页面路由由 `otworkflow.workflowApiRoutes` 自动注册，新增页面需在 `src/workflow/router/` 中定义
- 工作流 CSS 导入顺序至关重要，必须在 Element Plus CSS 之后导入
- `src/workflow/common-lib/` 下的库文件为第三方依赖，不应直接修改，通过升级方式更新
- WebSocket 连接需在组件卸载时调用 `disconnect()` 释放资源
- 新增工作流页面后需重启开发服务器（`pnpm run dev`），因为 `import.meta.glob` 在启动时扫描文件
- 如需在业务模块中集成审批功能，参考 `cdp-rule-workflow-fe.md` 规则中的代码模式
