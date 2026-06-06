# 业务模块开发手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 业务模块, cdp-common-frame, system, form, export, gen, job, 新增模块

---

## 一、业务模块总览

源码位置：`src/cdp-common-frame/`

| 模块 | 目录 | 职责 |
|------|------|------|
| common | `common/` | 登录页、仪表盘、错误页（404/403）、重定向 |
| system | `system/` | 用户、角色、组织、字典、资源、数据权限、安全设置、自动编号、假日管理 |
| form | `form/` | 自定义表单设计器（form-making 引擎） |
| fulltext | `fulltext/` | 全文检索 |
| gen | `gen/` | 代码生成器 |
| job | `job/` | 定时任务调度（XXL-Job） |
| export | `export/` | 数据导出管理 |
| report | `report/` | 报表（ureport 集成） |
| api-manage | `api-manage/` | API Key 管理、签名管理 |
| org-user-sync | `org-user-sync/` | 组织用户同步 |
| sms-platform | `sms-platform/` | 短信平台配置 |
| system-logs | `system-logs/` | 系统日志、操作日志、登录日志 |
| demo | `demo/` | 示例代码 |

---

## 二、标准模块目录结构

```
cdp-common-frame/模块名/
├── api/                   API 接口定义
│   ├── index.ts           API 方法（export function xxxApi）
│   ├── types.ts           TypeScript 请求/响应类型
│   └── all.ts             模块 API 聚合导出（可选）
│
├── views/                 页面组件
│   ├── index.vue          列表页（主页面）
│   ├── detail.vue         详情页（可选）
│   └── form.vue           表单页（可选）
│
├── components/            模块内部组件（可选）
│   └── XxxDialog.vue      对话框等局部组件
│
└── router/                模块路由（可选，部分模块用动态路由）
    ├── constantRoutes.ts  静态路由
    └── asyncRoutes.ts     动态路由定义
```

---

## 三、system 模块详解（最大业务模块）

源码位置：`src/cdp-common-frame/system/`

### API 模块列表

| API 模块 | 文件 | 功能 |
|---------|------|------|
| resource | `api/resource/` | 资源/菜单管理 CRUD |
| role | `api/role/` | 角色管理 CRUD |
| user | `api/user/` | 用户管理 CRUD |
| orgunit | `api/orgunit/` | 组织机构管理 |
| dictionary-kind | `api/dictionary-kind/` | 字典分类管理 |
| dictionary-value | `api/dictionary-value/` | 字典值管理 |
| holiday | `api/holiday/` | 假日管理 |
| autonumber | `api/autonumber/` | 自动编号规则 |
| autonumber-count | `api/autonumber-count/` | 自动编号计数 |
| data-resource | `api/data-resource/` | 数据权限资源 |
| security-setting | `api/security-setting/` | 安全设置 |

### API 编写示例（以 user 为例）

**types.ts:**

```typescript
// 用户查询参数
export interface UserQuery extends PageQuery {
  keywords?: string;
  status?: number;
  orgunitId?: string;
}

// 用户表单
export interface UserForm {
  userId?: string;
  username: string;
  nickname: string;
  mobile?: string;
  email?: string;
  gender?: number;
  status?: number;
  roleIds?: string[];
}
```

**index.ts:**

```typescript
import { post, postJson, get, del } from "@/cdp-common/utils/request";
import { UserQuery, UserForm } from "./types";

// 用户分页列表
export function listUserPages(queryParams: UserQuery) {
  return postJson("/system/user/page", queryParams);
}

// 用户详情
export function getUserDetail(userId: string) {
  return get(`/system/user/${userId}/detail`);
}

// 新增用户
export function addUser(data: UserForm) {
  return postJson("/system/user", data);
}

// 修改用户
export function updateUser(data: UserForm) {
  return postJson("/system/user/edit", data);
}

// 删除用户
export function deleteUsers(ids: string) {
  return del(`/system/user/${ids}`);
}
```

---

## 四、新增业务模块步骤

### Step 1: 创建目录结构

```bash
# 在 cdp-common-frame/ 下创建
mkdir -p src/cdp-common-frame/my-module/api
mkdir -p src/cdp-common-frame/my-module/views
```

### Step 2: 定义 API 类型

```typescript
// src/cdp-common-frame/my-module/api/types.ts
export interface MyItemQuery extends PageQuery {
  keyword?: string;
  status?: number;
}

export interface MyItemForm {
  id?: string;
  name: string;
  description?: string;
}
```

### Step 3: 编写 API 接口

```typescript
// src/cdp-common-frame/my-module/api/index.ts
import { postJson, get, del } from "@/cdp-common/utils/request";
import { MyItemQuery, MyItemForm } from "./types";

export function listMyItems(params: MyItemQuery) {
  return postJson("/my-module/page", params);
}

export function getMyItem(id: string) {
  return get(`/my-module/${id}`);
}

export function saveMyItem(data: MyItemForm) {
  return postJson("/my-module/save", data);
}

export function deleteMyItem(ids: string) {
  return del(`/my-module/${ids}`);
}
```

### Step 4: 创建页面

```vue
<!-- src/cdp-common-frame/my-module/views/index.vue -->
<script setup lang="ts">
import { listMyItems, deleteMyItem } from "../api";
import { MyItemQuery } from "../api/types";

const queryParams = reactive<MyItemQuery>({
  pageNum: 1,
  pageSize: 10,
});
const tableData = ref([]);
const total = ref(0);
const loading = ref(false);

async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await listMyItems(queryParams);
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

onMounted(() => handleQuery());
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="请输入" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-hasPerm="['my-module:edit']" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPerm="['my-module:delete']" type="danger" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>
```

### Step 5: 配置路由

**方式 A: 动态路由（推荐）**

在后端资源管理中配置菜单，`pagePath` 填写 `cdp-common-frame/my-module/views/index`（不含 `.vue`）。

**方式 B: 静态路由**

```typescript
// src/cdp-common-frame/my-module/router/constantRoutes.ts
export const constantRoutes = [
  {
    path: "/my-module",
    component: () => import("@/cdp-admin/layout/index.vue"),
    children: [
      {
        path: "",
        component: () => import("../views/index.vue"),
        meta: { title: "我的模块", icon: "list" },
      },
    ],
  },
];
```

然后在 `src/_changeable/router.ts` 中聚合该路由。

### Step 6: API 聚合（可选）

如需通过 `this.$api` 全局访问：

```typescript
// src/_changeable/api.ts
import * as myModule from "@/cdp-common-frame/my-module/api";

export default {
  ...cdpFrameApi,
  myModule,
};
```

---

## 五、API 聚合链路

```
各模块 api/index.ts
  → cdp-common-frame/xxx/api/all.ts（模块级聚合）
  → _changeable/api.ts（全局聚合）
  → main.ts: app.config.globalProperties.$api = api
  → 组件中: this.$api.system.resource.xxx() 或直接 import 使用
```

> 注意: 推荐直接 `import` API 函数使用，`$api` 全局方式缺乏 TypeScript 类型支持。

---

## 六、常见陷阱

1. **pagePath 路径**: 动态路由的 `pagePath` 相对于 `src/`，不含 `.vue` 后缀
2. **API 选择**: JSON 数据用 `postJson`，表单数据用 `post`，不要混用
3. **分页类型**: 使用 `PageQuery` 和 `PageResult<T>` 标准类型（定义在 `src/typings/global.d.ts`）
4. **权限标识**: `v-hasPerm` 的值来自后端 `getUserInfoApi` 返回的 `permissions` 数组
5. **直接 import vs $api**: 推荐直接 `import { xxxApi } from "./api"` 方式，有完整类型支持
