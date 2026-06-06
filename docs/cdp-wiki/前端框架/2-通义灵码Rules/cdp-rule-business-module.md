---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/09-module-business-frame.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-business-module.md
---

# 业务模块开发规范

## 适用场景

本规则适用于 CDP Web 前端框架中业务模块的开发任务，包括：
- 新增业务模块目录结构创建
- 页面组织（列表页、详情页、表单页）
- CRUD 模板（增删改查标准页面）
- API 接口定义与类型声明
- 路由骨架配置（静态路由 / 动态路由）
- 模块间通信与 API 聚合

## 前置依赖

- 包管理器仅限 **pnpm**
- 路径别名 `@/` 映射到 `src/`
- 全局类型 `PageQuery`、`PageResult<T>` 定义在 `src/typings/global.d.ts`
- 全局组件 `CdpTable`、`Pagination`、`SvgIcon` 已在 `main.ts` 注册
- 权限指令 `v-hasPerm` 已全局注册
- HTTP 请求封装位于 `@/cdp-common/utils/request`（提供 `postJson`、`get`、`post`、`del`、`getExport`）
- Element Plus 组件通过 unplugin-vue-components 自动按需导入

## 配置要点

### 标准模块目录结构

```
src/cdp-common-frame/{module-name}/
├── api/                         API 接口定义
│   └── {entity}/
│       ├── index.ts             API 方法（export function xxxApi）
│       └── types.ts             TypeScript 请求/响应类型
├── views/                       页面组件
│   ├── index.vue                列表页（主页面）
│   ├── detail.vue               详情页（可选）
│   └── components/              页面级子组件（可选）
│       └── XxxDialog.vue
└── router/                      模块路由（可选）
    ├── index.ts                 路由导出聚合
    ├── constantRoutes.ts        静态路由
    └── asyncRoutes.ts           动态路由定义
```

### 命名约定

| 类型 | 规范 | 示例 |
|------|------|------|
| 模块目录名 | kebab-case | `order-manage`、`api-manage` |
| API 目录/文件 | kebab-case | `dictionary-kind/`、`index.ts` |
| Vue 页面文件 | kebab-case 或 index.vue | `index.vue`、`detail.vue` |
| Vue 组件文件 | PascalCase | `OrderDialog.vue` |
| API 函数名 | 动词 + 名词 | `listOrders`、`addOrder`、`deleteOrder` |
| 类型/接口名 | PascalCase | `OrderQuery`、`OrderForm`、`OrderVO` |

### 新增模块六步流程

1. **创建目录结构** — `api/`、`views/`、`router/`
2. **定义 API 类型** — `types.ts` 中声明 Query、Form、VO 接口
3. **编写 API 接口** — `index.ts` 中导出各接口方法
4. **创建页面组件** — `views/index.vue` 列表页
5. **注册路由** — 静态路由或动态路由（推荐动态路由由后端菜单控制）
6. **聚合导出** — 路由聚合到 `_changeable/router.ts`，API 聚合到 `_changeable/api.ts`（可选）

## 代码模式

### 推荐写法

#### API 类型定义（types.ts）

```typescript
export interface OrderQuery extends PageQuery { orderNo?: string; status?: number; }
export interface OrderForm { id?: number; orderNo: string; customerName: string; amount: number; }
export interface OrderVO { id: number; orderNo: string; customerName: string; amount: number; status: number; createTime: string; }
```

#### API 接口定义（index.ts）

```typescript
import { postJson, get, del, getExport } from "@/cdp-common/utils/request";
import { OrderQuery, OrderForm } from "./types";

export function listOrders(params: OrderQuery) { return postJson("/order/listPage", params); }
export function getOrderDetail(id: string) { return get(`/order/${id}/detail`); }
export function addOrder(data: OrderForm) { return postJson("/order/add", data); }
export function updateOrder(data: OrderForm) { return postJson("/order/update", data); }
export function deleteOrder(ids: string) { return del(`/order/${ids}`); }
export function exportOrders(params: OrderQuery) { return getExport("/order/export", params); }
```

#### 列表页关键模式

```vue
<script setup lang="ts">
import { listOrders, deleteOrder } from "../api/order";
import type { OrderQuery, OrderVO } from "../api/order/types";

const queryParams = reactive<OrderQuery>({ pageNum: 1, pageSize: 20 });
const tableData = ref<OrderVO[]>([]);
const total = ref(0);
const loading = ref(false);

async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await listOrders(queryParams);
    tableData.value = data.list;
    total.value = data.total;
  } finally { loading.value = false; }
}
onMounted(() => handleQuery());
</script>
<template>
  <div class="app-container">
    <!-- 搜索栏: el-form :inline + el-input/el-select -->
    <!-- 表格: el-table v-loading + el-table-column，操作列用 v-hasPerm -->
    <!-- 分页: <Pagination v-model:total/page/limit @pagination="handleQuery" /> -->
  </div>
</template>
```

#### 路由注册

**静态路由**: `constantRoutes` 中定义 `path` + `component: Layout` + `children`，聚合到 `_changeable/router.ts`。

**动态路由（推荐）**: 后端资源管理配置菜单，`pagePath` 填 `cdp-common-frame/{module}/views/index`（不含 `.vue`），由 `import.meta.glob` 自动加载。

#### 模块间通信

```typescript
// 推荐：直接 import（有完整类型支持）
import { listOrders } from "@/cdp-common-frame/order/api/order";
// 不推荐：$api 全局访问（缺乏类型支持）
```

### 禁止事项

1. **禁止使用 Options API** — 必须使用 `<script setup lang="ts">`
2. **禁止直接使用 axios** — 必须使用 `@/cdp-common/utils/request` 封装方法
3. **禁止 JSON 请求使用 `post`** — JSON 数据用 `postJson`，表单数据用 `post`，不可混用
4. **禁止跨模块直接引用内部文件** — 使用 `@/` 路径别名，模块间通过导出的 API 通信
5. **禁止在 `pagePath` 中包含 `.vue` 后缀** — 动态路由 pagePath 相对于 `src/`，不含后缀
6. **禁止不声明类型直接使用 `any`** — 使用 `Record<string, any>` 或定义明确接口
7. **禁止在 Component 层直接调用 API** — 通过 Props/Emits 与 View 层通信
8. **禁止模块间循环依赖** — 下层模块不可引用上层模块
9. **禁止手动 `@import` SCSS 全局变量** — 已由 Vite additionalData 自动注入
10. **禁止忘记重启开发服务器** — 新增文件后需重启 `pnpm run dev`，`import.meta.glob` 在启动时扫描
