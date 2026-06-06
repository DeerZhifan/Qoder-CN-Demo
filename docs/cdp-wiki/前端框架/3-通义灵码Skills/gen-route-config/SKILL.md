---
type: gen
description: 基于 CDP 前端框架规范，生成路由配置文件
---

# 生成路由配置

## 输入参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| 模块名称 | 是 | 模块英文标识，用于目录和路由命名 | `data-analysis` |
| 路由路径 | 是 | 路由 URL 路径（以 `/` 开头） | `/data-analysis` |
| 页面组件列表 | 是 | 模块包含的页面名称列表 | `index, detail, create` |
| 是否需要权限控制 | 否 | 默认为动态路由（需权限），设为否则生成静态路由 | `是` |

## 执行步骤

### 第 1 步：创建路由配置文件

根据是否需要权限控制，生成对应类型的路由配置。

#### 静态路由模板（无需权限控制）

创建文件 `src/{模块名称}/router/constantRoutes.ts`：

```typescript
import type { RouteRecordRaw } from "vue-router";

const Layout = () => import("@/cdp-admin/layout/index.vue");

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "{路由路径}",
    component: Layout,
    children: [
      // 为每个页面组件生成子路由
      {
        path: "",
        name: "{页面Name}",
        component: () => import("@/{模块名称}/views/{页面文件路径}/index.vue"),
        meta: {
          title: "{页面标题}",
          icon: "document",
          hidden: false,
          affix: false,
          keepAlive: false,
        },
      },
      // 如有多个页面，后续子路由 path 使用具体路径
      {
        path: "{子页面路径}",
        name: "{子页面Name}",
        component: () => import("@/{模块名称}/views/{子页面文件路径}/index.vue"),
        meta: {
          title: "{子页面标题}",
          hidden: true, // 详情页等通常隐藏
        },
      },
    ],
  },
];
```

#### 动态路由说明（需权限控制）

动态路由由后端 `initMenu` 接口返回，无需前端定义路由配置文件。需要完成以下操作：
1. 在后台资源管理中配置菜单资源
2. `pagePath` 填写相对于 `src/` 的组件路径（不含 `.vue` 后缀），如 `{模块名称}/views/{页面名}/index`
3. 配置 `resourceId` 作为路由 `name`，同时用于接口鉴权的 `X-Resource-Id` 请求头

### 第 2 步：注册到路由聚合入口

仅静态路由需要此步骤。编辑 `src/_changeable/router.ts`，导入并合并新模块路由：

```typescript
// src/_changeable/router.ts
import { constantRoutes as {模块驼峰名}Routes } from "@/{模块名称}/router";

export const constantRoutes = [
  ...cdpRoutes,
  ...{模块驼峰名}Routes,  // 新增
];
```

如果页面需要免登录访问，同时在 `src/permission.ts` 的白名单中添加路径：

```typescript
const whiteList = ["/login", "{路由路径}"];
```

### 第 3 步：配置权限标识

为需要按钮级权限控制的页面配置权限标识。

在页面组件中使用 `v-hasPerm` 指令：

```vue
<template>
  <div class="app-container">
    <!-- 操作按钮添加权限控制 -->
    <el-button v-hasPerm="['{模块权限前缀}:{资源}:add']" type="primary">
      新增
    </el-button>
    <el-button v-hasPerm="['{模块权限前缀}:{资源}:edit']" type="warning">
      编辑
    </el-button>
    <el-button v-hasPerm="['{模块权限前缀}:{资源}:delete']" type="danger">
      删除
    </el-button>
  </div>
</template>
```

权限标识命名规范：`模块:资源:操作`，如 `sys:user:add`、`data:report:export`。

## 完成后提醒

- [ ] 静态路由已在 `src/_changeable/router.ts` 中聚合注册
- [ ] 动态路由已在后台资源管理中配置菜单和 `pagePath`
- [ ] 页面组件文件已创建在对应的 `views/` 目录下
- [ ] 免登录页面已添加到 `src/permission.ts` 的 `whiteList`
- [ ] 按钮权限标识已通过 `v-hasPerm` 指令配置
- [ ] 确认路由 `name` 唯一，避免与现有路由冲突
- [ ] 重启开发服务器（`pnpm run dev`）以使新路由生效
