---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/04-module-routing-permission.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-routing-permission.md
  - docs/knowledge-base/5-代码片段库/static-route-add.ts
---

# CDP 路由与权限配置规范

## 适用场景

本规则适用于以下开发任务：
- 新增静态路由（登录页、错误页、公开页面）
- 配置动态路由（业务菜单页面，由后端 `initMenu` 接口返回）
- 使用权限指令控制按钮级显隐（`v-hasPerm`、`v-hasRole`）
- 修改路由守卫逻辑（`src/permission.ts`）
- 配置白名单路由或 CAS 单点登录集成
- 理解菜单生成机制与组件动态加载

## 前置依赖

- 路由模式：Hash 模式（`createWebHashHistory`），URL 格式 `http://host/#/path`
- 路由聚合入口：`src/_changeable/router.ts`，合并所有模块的 constantRoutes 和 asyncRoutes
- 路由守卫：`src/permission.ts`，全局 beforeEach 前置守卫
- 权限 Store：`src/cdp-admin/store/modules/permission.ts`
- 权限指令：`src/cdp-common-ctrl/directive/permission/index.ts`
- Token 存储：Cookie 或 localStorage，通过 `cdp-token` HTTP 请求头发送

## 配置要点

### 静态路由（constantRoutes）

静态路由在应用启动时加载，无需权限控制。在模块的 `router/constantRoutes.ts` 中定义，然后在 `_changeable/router.ts` 中聚合。

### 动态路由（asyncRoutes）

动态路由由后端 `/system/resource/initMenu` 接口返回，登录后首次导航时加载。组件通过 `import.meta.glob("../../../**/**.vue")` 动态导入，`pagePath` 相对于 `src/` 且不含 `.vue` 后缀。

### 路由守卫流程

```
beforeEach → URL Token 注入检测 → CAS Ticket 检测 → Token 检查
  有 Token + 有路由 → 放行
  有 Token + 无路由 → getUserInfo + generateRoutes + addRoute → 重新导航
  无 Token → CAS 模式跳 CAS 登录 / 普通模式检查白名单或跳 /login
```

### 菜单生成

后端返回菜单数据 → `filterAsyncRoutes()` 递归遍历 → `getRoute()` 转换每一项：
- `pagePath="Layout"` → 主布局组件
- `pagePath="RouterView"` → 嵌套路由容器
- 其他路径 → `modules[\`../../../${pagePath}.vue\`]` 动态匹配

## 代码模式

### 推荐写法

#### 添加带 Layout 的静态路由

```typescript
// src/my-module/router/constantRoutes.ts
import type { RouteRecordRaw } from "vue-router";
const Layout = () => import("@/cdp-admin/layout/index.vue");

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/my-page",
    component: Layout,
    children: [
      {
        path: "",
        name: "MyPage",
        component: () => import("@/cdp-common-frame/my-module/views/index.vue"),
        meta: {
          title: "我的页面",
          icon: "document",
          hidden: false,
          affix: false,
          keepAlive: false,
        },
      },
    ],
  },
];
```

#### 添加独立页面（无 Layout）

```typescript
const routeStandalone: RouteRecordRaw = {
  path: "/public-page",
  name: "PublicPage",
  component: () => import("@/cdp-common-frame/my-module/views/public.vue"),
  meta: { title: "公开页面" },
};
```

#### 在聚合入口注册路由

```typescript
// src/_changeable/router.ts
import { constantRoutes as myRoutes } from "@/my-module/router";

export const constantRoutes = [
  ...cdpRoutes,
  ...myRoutes,  // 新增模块路由
];
```

#### 白名单配置（免登录页面）

```typescript
// src/permission.ts
const whiteList = ["/login", "/public-page"];
```

#### 按钮级权限控制

```vue
<template>
  <!-- 单个权限标识 -->
  <el-button v-hasPerm="['sys:user:add']" type="primary">新增</el-button>

  <!-- 多个权限标识，满足其一即可显示 -->
  <el-button v-hasPerm="['sys:user:edit', 'sys:user:update']">编辑</el-button>
</template>
```

#### 路由 meta 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `hidden` | `boolean` | 不在侧边栏菜单中显示 |
| `title` | `string` | 菜单标题 / 标签页标题 |
| `icon` | `string` | 菜单图标（SVG 图标名或 Element Plus 图标名） |
| `affix` | `boolean` | 标签页固定，不可关闭 |
| `keepAlive` | `boolean` | 组件是否缓存 |
| `actions` | `string[]` | 该页面可用的按钮权限标识列表 |

### 禁止事项

1. **禁止在前端代码中硬编码动态路由** — 业务菜单页面由后端 `initMenu` 接口返回，在后台资源管理中配置
2. **禁止省略 `next({ ...to, replace: true })` 中的 `replace: true`** — 否则路由守卫会无限循环
3. **禁止手动操作 permission store 判断按钮权限** — 使用 `v-hasPerm` 指令
4. **禁止在 `pagePath` 中包含 `.vue` 后缀** — `import.meta.glob` 匹配时自动拼接
5. **禁止在未聚合到 `_changeable/router.ts` 的情况下使用路由** — 不在聚合入口注册的路由不会生效
6. **禁止使用 `v-hasPerm` 做动态权限切换** — 该指令通过 `removeChild` 移除 DOM，不可恢复
7. **禁止忽略 ROOT 角色的特殊逻辑** — ROOT 角色自动拥有所有权限，跳过 `v-hasPerm` 校验
8. **禁止在白名单之外的路由省略登录检查** — 所有非白名单路由必须经过路由守卫的 Token 校验
