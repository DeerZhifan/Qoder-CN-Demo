# 路由与权限设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 路由, 权限, 路由守卫, 动态路由, CAS, v-hasPerm, permission.ts, 白名单

---

## 一、路由架构

### 基本配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 路由模式 | Hash (`createWebHashHistory`) | URL 格式: `http://host/#/path`，无需服务器配置 |
| 路由聚合 | `src/_changeable/router.ts` | 合并所有模块的 constantRoutes 和 asyncRoutes |
| 路由守卫 | `src/permission.ts` | beforeEach 全局前置守卫 |

### 路由分类

| 类型 | 来源 | 加载时机 | 示例 |
|------|------|---------|------|
| 静态路由（constantRoutes） | 代码定义 | 应用启动时 | `/login`、`/caslogin`、`/404`、`/401`、`/redirect` |
| 动态路由（asyncRoutes） | 后端接口 `/system/resource/initMenu` | 登录后首次导航时 | 用户菜单、系统管理、业务功能页面 |

### 路由聚合方式

```typescript
// src/_changeable/router.ts
import { constantRoutes as cdpRoutes, asyncRoutes as cdpAsyncRoutes } from "cdp-common-frame/router";
import { constantRoutes as demoRoutes } from "cdp-demo/router";
import workflowRouter from "workflow/router";
import otworkflow from "otworkflow";

export const constantRoutes = [
  ...cdpRoutes,
  ...demoRoutes,
  ...workflowRouter.constantRoutes,
  ...otworkflow.workflowApiRoutes,  // 工作流 API 路由
];

export const asyncRoutes = [...cdpAsyncRoutes];
```

---

## 二、动态路由加载机制

源码位置：`src/cdp-admin/store/modules/permission.ts`

### 核心流程

```
后端返回菜单数据 (listRoutes API)
  → filterAsyncRoutes() 递归遍历
  → getRoute() 转换每一项
    ├── pagePath="Layout"      → import("@/cdp-admin/layout/index.vue")
    ├── pagePath="RouterView"  → import("@/cdp-admin/layout/components/RouterView.vue")
    └── pagePath="其他路径"     → modules[`../../../${pagePath}.vue`]
        └── 未找到             → fallback 到 404 组件
  → setRoutes(constantRoutes + accessedRoutes)
  → router.addRoute() 逐条添加
```

### 组件动态加载

```typescript
// 使用 import.meta.glob 预收集所有 Vue 文件
const modules = import.meta.glob("../../../**/**.vue");

// 通过路径匹配组件
const component = modules[`../../../${item.pagePath}.vue`];
```

> 注意: `pagePath` 是相对于 `src/` 的路径（不含 `.vue` 后缀），如 `cdp-common-frame/system/views/user/index`。

### 路由项转换格式

```typescript
// 后端返回的菜单项 → 转换后的路由项
{
  path: item.openWithUrl || item.address,  // 外链或内部路径
  name: item.resourceId,                    // 资源 ID 作为路由名
  component: 动态加载的组件,
  meta: {
    hidden: item.hidden,       // 是否在菜单中隐藏
    icon: item.icon,           // 菜单图标
    title: item.label,         // 菜单标题
    actions: item.actions,     // 按钮权限标识列表
    layout: "Layout",          // 布局类型
  },
  children: 递归处理子路由,
}
```

---

## 三、路由守卫全流程

源码位置：`src/permission.ts`

```
router.beforeEach(to, from, next) →

① URL Token 注入检测
   getTokenFromUrl() 检查 URL 中是否有 cdp-token 参数
   ├── 有 → 存储 Token（Cookie/localStorage）
   │        → removeUrlParam() 清理 URL
   │        → history.replaceState() 避免刷新
   │        → next({ path: to.path, query: 去除 cdp-token })
   └── 无 → 继续

② CAS Ticket 检测
   URL 含 ticket= 或 token= 且目标是 /caslogin 或 /login
   ├── 是 → 直接放行 next()
   └── 否 → 继续

③ Token 检查
   globalConfig.sessionCookie ? getToken() : localStorage.getItem("cdp-token")

   ├── 有 Token
   │   ├── to.path === "/login" → 已登录，跳转首页 next("/")
   │   ├── permissionStore.routes.length > 0（已有路由）
   │   │   ├── to.matched.length === 0 → 未匹配，跳 404
   │   │   └── 正常放行 next()
   │   └── 无路由（首次进入或刷新）
   │       ├── userStore.getUserInfo()     获取用户信息
   │       ├── permissionStore.generateRoutes()  生成动态路由
   │       ├── router.addRoute() 逐条注册
   │       └── next({ ...to, replace: true })  重新导航
   │           └── 异常 → "Network Error" 忽略 / 其他 → resetToken + 跳登录
   │
   └── 无 Token
       ├── 清空 menu + userInfo
       ├── globalConfig.casStatus === true（CAS 模式）
       │   ├── 目标是 caslogin → 放行
       │   └── 其他 → location.href = getCasLoginUrl()
       └── 普通登录模式
           ├── whiteList 包含 to.path → 放行
           └── 跳转 /login?redirect=${to.path}
```

### 白名单配置

```typescript
const whiteList = ["/login"];  // 未登录可访问的路由
```

> 注意: 如需添加免登录页面，将路径加入 `whiteList` 数组即可。

---

## 四、CAS 单点登录集成

### 配置项

| 配置 | 位置 | 说明 |
|------|------|------|
| `globalConfig.casStatus` | config.js | CAS 总开关（true=CAS, false=普通登录）|
| `globalConfig.casServerType` | config.js | CAS 类型: 1=框架CAS, 2=新版集团CAS, 3=旧版, 4=CDP代理 |

### CAS 登录流程

```
用户访问受保护页面
  → permission.ts 检测无 Token
  → casStatus === true
  → location.href = getCasLoginUrl(defaultRouterName, to.path)
  → 浏览器跳转到 CAS 登录页
  → CAS 认证成功，回调带 ticket 参数
  → permission.ts 检测到 ticket，放行到 /caslogin
  → caslogin 页面用 ticket 换取 CDP token
  → 存储 Token，重定向到目标页面
```

### CAS 退出流程

```
401 响应或主动退出
  → globalConfig.casStatus === true
  → location.href = getCasLogoutUrl(isTimeOut)
  → 跳转 CAS 注销页面
```

---

## 五、权限指令

源码位置：`src/cdp-common-ctrl/directive/permission/index.ts`

### v-hasPerm — 按钮级权限控制

```vue
<!-- 用法：传入权限标识数组 -->
<el-button v-hasPerm="['sys:user:add']">新增用户</el-button>
<el-button v-hasPerm="['sys:user:edit', 'sys:user:update']">编辑</el-button>
```

**工作原理:**

1. 从 `useUserStoreHook().user` 获取 `roles` 和 `permission`
2. 如果 `roles` 包含 `"ROOT"` → 超级管理员，直接放行
3. 否则检查 `permission` 数组中是否有任意一项被 `value` 包含
4. 无权限 → `el.parentNode.removeChild(el)` 直接移除 DOM 节点

### v-hasRole — 角色级权限控制

```vue
<el-button v-hasRole="['admin', 'editor']">管理员操作</el-button>
```

**工作原理:** 检查 `user.roles` 中是否有任意一项被 `value` 包含。

### 注册方式

```typescript
// src/cdp-common-ctrl/directive/index.ts
export function setupDirective(app) {
  app.directive("hasPerm", hasPerm);
}

// src/main.ts
setupDirective(app);
```

> 注意: 当前只注册了 `v-hasPerm`，`v-hasRole` 未在 `setupDirective` 中全局注册。若需使用 `v-hasRole`，需手动添加注册。

---

## 六、路由 meta 字段说明

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `hidden` | `boolean` | 后端 | 不在侧边栏菜单中显示 |
| `title` | `string` | 后端 (label) | 菜单标题 / 标签页标题 |
| `icon` | `string` | 后端 | 菜单图标（SVG 图标名或 Element Plus 图标名） |
| `affix` | `boolean` | 前端定义 | 标签页固定，不可关闭 |
| `keepAlive` | `boolean` | 前端定义 | 组件是否缓存（配合 `<keep-alive>`） |
| `actions` | `string[]` | 后端 | 该页面可用的按钮权限标识列表 |
| `layout` | `string` | 前端生成 | 页面布局类型 |

### actions 字段与 v-hasPerm 的关系

```
后端 initMenu 返回:
  { resourceId: "sys-user", label: "用户管理", actions: ["sys:user:add", "sys:user:edit", "sys:user:delete"] }

前端 v-hasPerm 校验:
  user.permission = ["sys:user:add", "sys:user:edit"]  ← 来自 getUserInfoApi()
  <button v-hasPerm="['sys:user:delete']">  ← 无权限，DOM 被移除
```

---

## 七、常见陷阱

1. **新页面不显示在菜单**: 动态路由由后端 `initMenu` 接口返回，需在后端资源管理中配置
2. **路由守卫死循环**: `next({ ...to, replace: true })` 中的 `replace: true` 不能省略，否则会无限循环
3. **刷新后路由丢失**: 动态路由存储在内存中，刷新后重新从接口加载。如果接口异常会导致路由丢失
4. **组件路径匹配失败**: `import.meta.glob` 使用相对路径 `../../../**/**.vue`，`pagePath` 必须相对于 `src/` 且不含 `.vue`
5. **v-hasPerm 不生效**: 检查后端返回的 `permissions` 数组是否包含对应标识，ROOT 角色跳过校验
6. **免登录页面**: 将路径加入 `permission.ts` 的 `whiteList` 数组
7. **CAS 登录跳不回来**: 检查 `globalConfig.casServerType` 和对应 CAS 服务器地址配置
