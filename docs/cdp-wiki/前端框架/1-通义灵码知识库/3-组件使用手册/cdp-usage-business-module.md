# 如何新增业务模块

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 新增模块, 业务模块, cdp-common-frame, 目录结构, 路由注册, API聚合, 模块开发

---

## 概述

CDP Web 的业务模块位于 `src/cdp-common-frame/` 目录下，每个模块包含 API 定义、页面视图、路由配置三部分。新增业务模块需要完成 6 个步骤：创建目录 → 定义类型 → 编写 API → 创建页面 → 注册路由 → 聚合导出。

## 现有业务模块

| 模块 | 目录 | 功能 |
|------|------|------|
| system | `cdp-common-frame/system/` | 系统管理（用户、角色、组织、资源、字典、设置） |
| form | `cdp-common-frame/form/` | 自定义表单 |
| job | `cdp-common-frame/job/` | 定时任务管理 |
| export | `cdp-common-frame/export/` | 数据导出 |
| fulltext | `cdp-common-frame/fulltext/` | 全文检索 |
| gen | `cdp-common-frame/gen/` | 代码生成器 |
| report | `cdp-common-frame/report/` | 报表管理 |
| sms-platform | `cdp-common-frame/sms-platform/` | 短信平台 |
| api-manage | `cdp-common-frame/api-manage/` | API 接口管理 |
| system-logs | `cdp-common-frame/system-logs/` | 系统日志 |
| org-user-sync | `cdp-common-frame/org-user-sync/` | 组织用户同步 |

## 完整新增步骤

以新增"订单管理"模块为例：

### 第 1 步：创建目录结构

```
src/cdp-common-frame/order/
├── api/
│   └── order/
│       ├── index.ts        # API 接口方法
│       └── types.ts         # 类型定义
├── views/
│   ├── index.vue            # 列表页
│   └── detail.vue           # 详情页（可选）
└── router/
    ├── index.ts             # 路由导出
    ├── constantRoutes.ts    # 静态路由（可选）
    └── asyncRoutes.ts       # 动态路由（可选）
```

### 第 2 步：定义类型

```typescript
// src/cdp-common-frame/order/api/order/types.ts
export interface OrderQuery {
  orderNo?: string;
  status?: number;
  pageNum: number;
  pageSize: number;
}

export interface OrderForm {
  id?: number;
  orderNo: string;
  customerName: string;
  amount: number;
}

export interface OrderVO {
  id: number;
  orderNo: string;
  customerName: string;
  amount: number;
  status: number;
  createTime: string;
}
```

### 第 3 步：编写 API

```typescript
// src/cdp-common-frame/order/api/order/index.ts
import * as request from "@/cdp-common/utils/request";
import { OrderQuery, OrderForm } from "./types";

// 分页查询
export function listOrders(params: OrderQuery) {
  return request.postJson("/order/listPage", params);
}

// 新增
export function addOrder(data: OrderForm) {
  return request.postJson("/order/add", data);
}

// 修改
export function updateOrder(data: OrderForm) {
  return request.postJson("/order/update", data);
}

// 删除
export function deleteOrder(ids: string) {
  return request.post(`/order/delete/${ids}`);
}

// 导出
export function exportOrders(params: OrderQuery) {
  return request.getExport("/order/export", params);
}
```

### 第 4 步：创建页面

```vue
<!-- src/cdp-common-frame/order/views/index.vue -->
<script setup lang="ts">
import { listOrders, deleteOrder } from "../api/order";
import type { OrderQuery, OrderVO } from "../api/order/types";

const queryParams = reactive<OrderQuery>({
  pageNum: 1,
  pageSize: 20,
});
const tableData = ref<OrderVO[]>([]);
const total = ref(0);

async function getTableData() {
  const { data } = await listOrders(queryParams);
  tableData.value = data.records;
  total.value = data.total;
}

onMounted(() => getTableData());
</script>

<template>
  <CdpTable ref="cdpTable" @getTableData="getTableData">
    <template #queryBar>
      <el-form-item label="订单号">
        <el-input v-model="queryParams.orderNo" />
      </el-form-item>
    </template>
    <template #default>
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="customerName" label="客户" />
      <el-table-column prop="amount" label="金额" />
    </template>
  </CdpTable>
</template>
```

### 第 5 步：注册路由

```typescript
// src/cdp-common-frame/order/router/index.ts
export * from "./constantRoutes";
export * from "./asyncRoutes";
```

```typescript
// src/cdp-common-frame/order/router/constantRoutes.ts
const Layout = () => import("@/cdp-admin/layout/index.vue");

export const constantRoutes = [
  {
    path: "/order",
    component: Layout,
    meta: { title: "订单管理", icon: "document" },
    children: [
      {
        path: "",
        name: "OrderList",
        component: () => import("@/cdp-common-frame/order/views/index.vue"),
        meta: { title: "订单列表" },
      },
    ],
  },
];
```

```typescript
// src/cdp-common-frame/order/router/asyncRoutes.ts
export const asyncRoutes = [];  // 动态路由由后端菜单管理，通常为空
```

### 第 6 步：聚合到 _changeable

路由聚合：

```typescript
// src/_changeable/router.ts
import * as orderRouter from "@/cdp-common-frame/order/router";

export const constantRoutes = [
  ...cdpCommonFrame.constantRoutes,
  ...orderRouter.constantRoutes,  // 新增
];
```

API 聚合（如果需要通过 `$api` 全局访问）：

```typescript
// src/cdp-common-frame/system/api/all.ts 或新建聚合文件
export * as orderApi from "@/cdp-common-frame/order/api/order";
```

## 注意事项

> 注意：模块目录名使用 kebab-case（如 `order-manage`），Vue 文件使用 PascalCase 或 kebab-case，API 文件使用 camelCase。

> 注意：如果页面路由由后端动态菜单控制，只需确保 Vue 文件路径与后端菜单配置的 `pagePath` 一致，路由会自动通过 `import.meta.glob` 加载。

> 注意：新增模块后需要重启开发服务器（`pnpm run dev`），因为 `import.meta.glob` 在启动时扫描文件。

> 注意：每个模块应保持独立性，API 定义、类型、视图都放在模块目录内，避免跨模块直接引用内部文件。
