# 工作流集成设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 工作流, workflow, OT, otworkflow, 流程, 审批, 待办

---

## 一、模块结构

源码位置：`src/workflow/`

```
workflow/
├── api/                     30+ 工作流 API
│   ├── index.js             API 聚合导出
│   ├── tasks.js             待办/已办/任务查询
│   ├── monitor.js           流程监控
│   ├── bill.js              单据管理
│   ├── config.js            流程配置
│   ├── event.js             事件管理
│   ├── opinion.js           审批意见
│   ├── jobTemplate.js       作业模板
│   ├── transactor.js        办理人管理
│   ├── tasksurrogate.js     任务委托
│   ├── tag.js               标签管理
│   └── ...                  其他 API
│
├── views/                   50+ 工作流视图
│   ├── bill/                单据管理
│   ├── chart/               流程图
│   ├── config/              流程配置
│   ├── monitor/             流程监控
│   ├── opinion/             审批意见
│   ├── taskStatistics/      任务统计
│   ├── tasksurrogate/       任务委托
│   ├── transactor/          办理人
│   ├── acttemplate/         活动模板
│   ├── vartemplate/         变量模板
│   ├── decisionNode/        决策节点
│   └── ...                  其他视图
│
├── router/
│   ├── index.ts             路由聚合
│   ├── constantRoutes.js    工作流静态路由
│   └── asyncRoutes.js       工作流动态路由
│
├── utils/                   工作流工具函数
│
└── common-lib/              工作流依赖库
    ├── msdp-common/         MSDP 公共库
    ├── msdp-common-ctrl/    MSDP 控件库
    ├── msdp-common-nui/     MSDP NUI 组件
    ├── otworkflow/          OT 工作流引擎
    ├── otchart/             流程图组件
    ├── otfulltextsearch/    全文检索
    └── example/             示例
```

---

## 二、集成方式

### 依赖引入

```json
// package.json — 以 file: 协议引用本地库
{
  "dependencies": {
    "msdp-common": "file:src/workflow/common-lib/msdp-common",
    "msdp-common-ctrl": "file:src/workflow/common-lib/msdp-common-ctrl",
    "msdp-common-nui": "file:src/workflow/common-lib/msdp-common-nui",
    "otworkflow": "file:src/workflow/common-lib/otworkflow"
  }
}
```

### 插件注册

```typescript
// src/main.ts

// 1. 导入工作流插件
import otworkflow from "@/cdp-common-admin/components/otworkflow.js";

// 2. 导入工作流 CSS
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

### 路由集成

```typescript
// src/_changeable/router.ts
import workflowRouter from "@/workflow/router";
import otworkflow from "otworkflow";

export const constantRoutes = [
  ...cdpCommonFrameRoutes.constantRoutes,
  ...workflowRouter.constantRoutes,        // 工作流静态路由
  ...otworkflow.workflowApiRoutes,          // OT 工作流 API 路由
];
```

---

## 三、工作流 API 分类

| 分类 | 文件 | 主要接口 |
|------|------|---------|
| 任务管理 | `tasks.js` | 待办列表、已办列表、任务详情、任务处理 |
| 流程监控 | `monitor.js` | 流程实例列表、流程状态、流程跟踪 |
| 单据管理 | `bill.js` | 单据模板、单据创建、单据查询 |
| 流程配置 | `config.js` | 流程定义、节点配置、条件配置 |
| 事件管理 | `event.js` | 事件定义、事件触发 |
| 审批意见 | `opinion.js` | 意见模板、常用意见 |
| 作业模板 | `jobTemplate.js` | 模板定义、模板应用 |
| 办理人 | `transactor.js` | 办理人配置、预测办理人 |
| 任务委托 | `tasksurrogate.js` | 委托配置、委托查询 |
| 标签管理 | `tag.js` | 流程标签 |

---

## 四、工作流组件体系

工作流有独立的组件系统，与主系统并行：

| 组件库 | 说明 | 注册方式 |
|--------|------|---------|
| msdp-common-nui | 工作流 NUI 组件 | CSS 导入 |
| msdp-common-ctrl | 工作流控件 | `import.meta.glob` 自动注册 |
| otworkflow | OT 工作流核心 | `app.use()` 插件注册 |
| otchart | 流程图组件 | 按需导入 |

---

## 五、常见陷阱

1. **file: 协议依赖**: 工作流库通过 `file:` 协议引用本地目录，`pnpm install` 后创建软链接，修改源码即时生效
2. **CSS 导入顺序**: 工作流 CSS 必须在 Element Plus CSS 之后导入，否则样式可能被覆盖
3. **组件名冲突**: msdp-common-ctrl 组件通过 `import.meta.glob` 注册，可能与主系统组件名冲突
4. **路由合并**: 工作流路由通过 `_changeable/router.ts` 聚合，新增工作流页面需在 workflow/router 中定义
5. **API 前缀**: 工作流 API 可能使用与主系统不同的前缀，注意 Vite Proxy 配置
