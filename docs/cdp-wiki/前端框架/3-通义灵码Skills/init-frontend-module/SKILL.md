---
type: init
description: 初始化一个完整的业务模块目录结构，包含 API、页面、路由等骨架文件
rule: cdp-rule-business-module.md
---

# 初始化前端业务模块

## 输入参数

| 参数 | 说明 | 示例 |
|------|------|------|
| {模块名称} | 模块英文名（kebab-case） | `order`、`contract-manage` |
| {实体名称} | 实体英文名（camelCase，用于 API 函数命名） | `order`、`contract` |
| {模块中文名} | 模块中文显示名称 | `订单管理`、`合同管理` |
| {所属菜单路径} | 路由挂载路径 | `/order`、`/contract` |

## 执行步骤

### 第 1 步：创建目录结构

创建以下目录和文件：

```
src/cdp-common-frame/{模块名称}/
├── api/
│   └── {实体名称}/
│       ├── index.ts
│       └── types.ts
├── views/
│   ├── index.vue
│   └── components/
└── router/
    ├── index.ts
    ├── constantRoutes.ts
    └── asyncRoutes.ts
```

### 第 2 步：创建 API 模块

**types.ts** — 类型定义：

```typescript
// src/cdp-common-frame/{模块名称}/api/{实体名称}/types.ts

/** {模块中文名} - 查询参数 */
export interface {Entity}Query extends PageQuery {
  keyword?: string;
  status?: number;
}

/** {模块中文名} - 表单数据 */
export interface {Entity}Form {
  id?: string;
  name: string;
  // TODO: 根据业务补充字段
}

/** {模块中文名} - 列表项 */
export interface {Entity}VO {
  id: string;
  name: string;
  status: number;
  createTime: string;
  // TODO: 根据业务补充字段
}
```

> 注意：`{Entity}` 为实体名称的 PascalCase 形式，如 `Order`、`Contract`。

**index.ts** — API 接口方法：

```typescript
// src/cdp-common-frame/{模块名称}/api/{实体名称}/index.ts
import { postJson, get, del, getExport } from "@/cdp-common/utils/request";
import type { {Entity}Query, {Entity}Form } from "./types";

/** 分页查询 */
export function list{Entity}Pages(params: {Entity}Query) {
  return postJson("/{模块名称}/listPage", params);
}

/** 详情 */
export function get{Entity}Detail(id: string) {
  return get(`/{模块名称}/${id}/detail`);
}

/** 新增 */
export function add{Entity}(data: {Entity}Form) {
  return postJson("/{模块名称}/add", data);
}

/** 修改 */
export function update{Entity}(data: {Entity}Form) {
  return postJson("/{模块名称}/update", data);
}

/** 删除（支持批量，ids 逗号分隔） */
export function delete{Entity}(ids: string) {
  return del(`/{模块名称}/${ids}`);
}

/** 导出 */
export function export{Entity}(params: {Entity}Query) {
  return getExport("/{模块名称}/export", params);
}
```

### 第 3 步：创建页面组件

**views/index.vue** — 列表页（含搜索 + 表格 + 分页 + 新增/编辑弹窗 + 删除确认）：

```vue
<!-- src/cdp-common-frame/{模块名称}/views/index.vue -->
<script setup lang="ts">
import {
  list{Entity}Pages,
  add{Entity},
  update{Entity},
  delete{Entity},
} from "../api/{实体名称}";
import type { {Entity}Query, {Entity}Form, {Entity}VO } from "../api/{实体名称}/types";

// ==================== 查询 ====================
const queryParams = reactive<{Entity}Query>({
  pageNum: 1,
  pageSize: 20,
});
const tableData = ref<{Entity}VO[]>([]);
const total = ref(0);
const loading = ref(false);

async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await list{Entity}Pages(queryParams);
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  queryParams.keyword = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  handleQuery();
}

// ==================== 新增/编辑 ====================
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref();
const formData = reactive<{Entity}Form>({
  name: "",
});

const rules = {
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
};

function handleAdd() {
  dialogTitle.value = "新增";
  Object.assign(formData, { id: undefined, name: "" });
  dialogVisible.value = true;
}

function handleEdit(row: {Entity}VO) {
  dialogTitle.value = "编辑";
  Object.assign(formData, { ...row });
  dialogVisible.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (formData.id) {
    await update{Entity}(formData);
  } else {
    await add{Entity}(formData);
  }
  ElMessage.success("操作成功");
  dialogVisible.value = false;
  handleQuery();
}

// ==================== 删除 ====================
async function handleDelete(row: {Entity}VO) {
  await ElMessageBox.confirm("确认删除该记录？", "提示", { type: "warning" });
  await delete{Entity}(row.id);
  ElMessage.success("删除成功");
  handleQuery();
}

// ==================== 初始化 ====================
onMounted(() => handleQuery());
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" class="mb-4">
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入关键词"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="mb-4">
      <el-button v-hasPerm="['{模块名称}:add']" type="primary" @click="handleAdd">
        新增
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? "启用" : "禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPerm="['{模块名称}:edit']" type="primary" link @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button v-hasPerm="['{模块名称}:delete']" type="danger" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      v-if="total > 0"
      v-model:total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="handleQuery"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>
        <!-- TODO: 根据业务补充表单字段 -->
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.mb-4 {
  margin-bottom: 16px;
}
</style>
```

### 第 4 步：注册路由

**router/index.ts** — 路由导出聚合：

```typescript
// src/cdp-common-frame/{模块名称}/router/index.ts
export * from "./constantRoutes";
export * from "./asyncRoutes";
```

**router/constantRoutes.ts** — 静态路由：

```typescript
// src/cdp-common-frame/{模块名称}/router/constantRoutes.ts
const Layout = () => import("@/cdp-admin/layout/index.vue");

export const constantRoutes = [
  {
    path: "{所属菜单路径}",
    component: Layout,
    meta: { title: "{模块中文名}", icon: "document" },
    children: [
      {
        path: "",
        name: "{Entity}List",
        component: () => import("@/cdp-common-frame/{模块名称}/views/index.vue"),
        meta: { title: "{模块中文名}" },
      },
    ],
  },
];
```

**router/asyncRoutes.ts** — 动态路由（通常为空，由后端菜单管理控制）：

```typescript
// src/cdp-common-frame/{模块名称}/router/asyncRoutes.ts
export const asyncRoutes = [];
```

**聚合到 `_changeable/router.ts`**：

```typescript
// 在 src/_changeable/router.ts 中添加
import * as {entity}Router from "@/cdp-common-frame/{模块名称}/router";

export const constantRoutes = [
  ...existingRoutes,
  ...{entity}Router.constantRoutes,
];
```

## 完成后提醒

- 新增模块后必须重启开发服务器（`pnpm run dev`），因为 `import.meta.glob` 在启动时扫描文件
- 如果使用动态路由（推荐），需在后端资源管理中配置菜单，`pagePath` 填写 `cdp-common-frame/{模块名称}/views/index`（不含 `.vue`）
- API 聚合到 `_changeable/api.ts` 是可选的，推荐直接 `import` API 函数使用（有完整类型支持）
- 模块目录名使用 kebab-case，实体名称使用 camelCase，类型名使用 PascalCase
- 权限标识（如 `{模块名称}:add`）需与后端 `getUserInfoApi` 返回的 `permissions` 数组一致
- 运行 `pnpm run lint:eslint` 检查代码规范
