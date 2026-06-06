# 如何配置路由和权限

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 路由, 权限, v-hasPerm, 动态路由, 静态路由, 路由守卫, CAS, 白名单, permission.ts

---

## 概述

CDP Web 使用 Vue Router Hash 模式，路由分为静态路由和动态路由两类。权限控制通过路由守卫（`src/permission.ts`）和自定义指令（`v-hasPerm`）实现。支持 CAS 单点登录和传统登录两种认证模式。

## 添加静态路由

静态路由无需登录即可访问（或固定显示），在各模块的 `router/constantRoutes.ts` 中定义，最终在 `src/_changeable/router.ts` 聚合。

### 第 1 步：在模块中定义路由

```typescript
// src/cdp-common-frame/router/constantRoutes.ts
const Layout = () => import("@/cdp-admin/layout/index.vue");

export const constantRoutes = [
  {
    path: "/my-page",
    component: Layout,
    children: [
      {
        path: "",
        name: "MyPage",
        component: () => import("@/cdp-common-frame/my-module/views/index.vue"),
        meta: { title: "我的页面", icon: "document", hidden: false },
      },
    ],
  },
];
```

### 第 2 步：在 _changeable/router.ts 中聚合

```typescript
// src/_changeable/router.ts
import * as myModule from "@/my-module/router";

export const constantRoutes = [
  ...cdpCommonFrame.constantRoutes,
  ...myModule.constantRoutes,  // 新增
];
```

## 动态路由

动态路由从后端 API (`listRoutes()`) 加载，根据用户权限过滤后通过 `router.addRoute()` 注册。在后台管理界面中配置菜单资源即可，无需修改前端代码。

动态路由组件解析规则：
- `pagePath = "Layout"` → 主布局组件
- `pagePath = "RouterView"` → 嵌套路由容器
- 其他 → 通过 `import.meta.glob("../../../**/**.vue")` 动态导入对应 Vue 文件

## 使用 v-hasPerm 权限指令

`v-hasPerm` 指令用于控制按钮级别的显示权限，无权限时直接移除 DOM 元素。

### 基本用法

```vue
<template>
  <!-- 需要 sys:user:add 权限才显示 -->
  <el-button v-hasPerm="['sys:user:add']">新增用户</el-button>

  <!-- 需要任意一个权限即可显示 -->
  <el-button v-hasPerm="['sys:user:edit', 'sys:user:update']">编辑</el-button>
</template>
```

### 工作原理

指令从 `useUserStoreHook().user` 中读取当前用户的 `permission` 数组，判断是否包含指定权限标识。超级管理员（角色包含 `ROOT`）自动拥有所有权限。

源码位置：`src/cdp-common-ctrl/directive/permission/index.ts`

## 白名单路由

白名单路由无需登录即可访问，在 `src/permission.ts` 中配置：

```typescript
const whiteList = ["/login"];
```

## CAS 单点登录

通过 `window.globalConfig.casStatus` 开关控制：

- `casStatus = true`：启用 CAS，未登录时跳转 CAS 登录页
- `casStatus = false`：使用传统用户名密码登录

CAS 相关工具函数位于 `src/cdp-common/utils/cas.ts`。

## URL Token 免登录

支持通过 URL 参数 `cdp-token` 实现免登录跳转：

```
http://域名/#/dashboard?cdp-token=xxx
```

路由守卫会自动提取 Token 并存储，然后清除 URL 中的参数防止泄露。

## 注意事项

> 注意：本项目使用 Hash 路由模式（`createWebHashHistory`），URL 格式为 `http://域名/#/path`。URL 参数在 `#` 之后，如 `/#/path?cdp-token=xxx`。

> 注意：`v-hasPerm` 是**移除 DOM**的方式控制权限，不是 `display: none`，被移除的元素无法恢复。适用于页面加载时的静态权限判断。

> 注意：动态路由的 `name` 字段使用资源 ID（`resourceId`），同时作为请求头 `X-Resource-Id` 的值用于后端接口鉴权。

> 注意：添加新模块的路由后，必须在 `src/_changeable/router.ts` 中导入并展开到 `constantRoutes` 或 `asyncRoutes` 数组中，否则路由不会生效。
