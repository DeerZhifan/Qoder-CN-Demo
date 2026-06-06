# 状态管理设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: Pinia, Store, 状态管理, useUserStore, usePermissionStore, useStorage, Hook

---

## 一、架构决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| Store 风格 | Composition API `defineStore` | 与 Vue 3 组合式 API 统一，TypeScript 类型推导更好 |
| 持久化方案 | `@vueuse/core` 的 `useStorage` | 轻量、声明式，直接在 Store 内使用，无需额外插件 |
| 外部调用模式 | `useXxxStoreHook()` | 传入预创建的 store 实例，解决组件外 Pinia 未初始化问题 |

---

## 二、5 个核心 Store 详解

### 2.1 user.ts — 用户认证与信息

源码位置：`src/cdp-admin/store/modules/user.ts`

**State:**

| 字段 | 类型 | 持久化 | 说明 |
|------|------|--------|------|
| `token` | `string` / `Ref<string>` | Cookie 或 localStorage | Cookie 模式为普通字符串，localStorage 模式为 `useStorage("cdp-token", "")` |
| `user` | `UserInfo` | 否 | 当前用户信息（roles/permission）|
| `userInfo` | `Ref<any>` | localStorage (`cdp-userInfo`) | 完整用户信息缓存 |
| `preLoginToken` | `Ref<string>` | localStorage (`cdp-preLoginToken`) | 二次登录 Token |
| `preLoginMode` | `Ref<string>` | localStorage (`cdp-preLoginMode`) | 二次登录模式 |

**Actions:**

```typescript
// 登录 — 处理 4 种响应码
login(loginData: LoginData): Promise<void>
  // code=200:  登录成功，存储 Token
  // code=10010: 跳转修改密码界面
  // code=10020: 跳转二次登录界面，存储 preLoginToken
  // code=10030: 二次登录 preLoginToken 过期

// 获取用户信息 — 含缓存逻辑
getUserInfo(): Promise<UserInfo>
  // 若 userInfo.value.userId 已存在 → 直接从 localStorage 恢复，不请求接口
  // 否则 → 调用 getUserInfoApi() 获取

// 退出登录
logout(): Promise<void>
  // 调用 logoutApi() → 清空 Token → location.reload()

// 清除 Token
resetToken(): Promise<void>
  // 清空 Token → resetRouter()
```

> 注意: `getUserInfo()` 刷新浏览器时从 `useStorage` 恢复缓存，避免重复请求。这意味着用户信息更新后需要清除 `cdp-userInfo` 缓存才能生效。

---

### 2.2 permission.ts — 动态路由管理

源码位置：`src/cdp-admin/store/modules/permission.ts`

**State:**

| 字段 | 类型 | 持久化 | 说明 |
|------|------|--------|------|
| `routes` | `Ref<RouteRecordRaw[]>` | 否 | 完整路由（constantRoutes + 动态路由） |
| `menu` | `Ref<any[]>` | localStorage (`cdp-menu`) | 后端返回的原始菜单数据 |
| `mixLeftMenu` | `Ref<RouteRecordRaw[]>` | 否 | 混合布局模式左侧菜单 |

**Actions:**

```typescript
// 生成动态路由
generateRoutes(): Promise<RouteRecordRaw[]>
  // 1. 检查 menu 缓存 → 有则直接用，不请求接口
  // 2. 调用 listRoutes() 获取后端菜单数据
  // 3. filterAsyncRoutes() 递归转换路由格式
  // 4. setRoutes() 合并 constantRoutes + 动态路由
```

**动态组件加载机制:**

```typescript
// 预收集所有 Vue 组件
const modules = import.meta.glob("../../../**/**.vue");

function getRoute(item) {
  if (pagePath == "Layout")      → import("@/cdp-admin/layout/index.vue")
  if (pagePath == "RouterView")  → import("@/cdp-admin/layout/components/RouterView.vue")
  else                           → modules[`../../../${pagePath}.vue`]
  // 未找到组件 → fallback 到 404 页面
}
```

---

### 2.3 settings.ts — 应用设置

**State（均通过 `useStorage` 持久化到 localStorage）:**

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `title` | "通用技术研发平台" | 系统标题 |
| `layout` | "left" | 布局模式: left / top / mix |
| `theme` | "light" | 主题: light / dark / blue |
| `themeColor` | "#409EFF" | Element Plus 主题色 |
| `watermark` | false | 水印开关 |
| `tagsView` | true | 标签页栏开关 |
| `language` | "zh-cn" | 语言 |

**主题切换:** `changeSetting()` 修改设置后，将主题类名应用到 `document.documentElement.className`。

---

### 2.4 app.ts — 应用全局状态

| 字段 | 说明 |
|------|------|
| `sidebar.opened` | 侧边栏展开/折叠 |
| `device` | 设备类型: desktop / mobile |
| `language` | 当前语言 |
| `locale` | Element Plus locale 对象 |
| `size` | UI 尺寸: default / small / large |
| `activeTopMenu` | 混合模式激活的顶部菜单路径 |

---

### 2.5 tagsView.ts — 标签页管理

| 字段 | 说明 |
|------|------|
| `visitedViews` | 已访问的标签页列表 |
| `cachedViews` | 需要 keep-alive 缓存的组件名列表 |

**Actions:** `addView` / `delView` / `delOthersViews` / `delAllViews` / `updateVisitedView`

---

## 三、使用模式

### 组件内使用（自动注入 Pinia 实例）

```vue
<script setup lang="ts">
import { useUserStore } from "@/cdp-admin/store/modules/user";

const userStore = useUserStore();
// 直接访问
console.log(userStore.user.roles);
// 调用 action
await userStore.getUserInfo();
</script>
```

### 组件外使用（路由守卫、拦截器、工具函数）

```typescript
// ❌ 错误：组件外直接调用会报错（Pinia 未初始化）
import { useUserStore } from "@/cdp-admin/store/modules/user";
const userStore = useUserStore(); // Error!

// ✅ 正确：使用 Hook 模式
import { useUserStoreHook } from "@/cdp-admin/store/modules/user";
const userStore = useUserStoreHook(); // OK
```

**Hook 模式实现原理:**

```typescript
import { store } from "@/cdp-admin/store";

// 导出 Hook 函数，传入已创建的 Pinia 实例
export function useUserStoreHook() {
  return useUserStore(store);
}
```

> 注意: 所有在 `permission.ts`（路由守卫）、`request.ts`（拦截器）中使用的 Store 都**必须**用 `useXxxStoreHook()` 模式。

---

## 四、持久化策略速查

| 数据 | 存储方式 | Key | Store |
|------|---------|-----|-------|
| Token | Cookie 或 localStorage | `cdp-token` | user |
| 用户信息 | localStorage | `cdp-userInfo` | user |
| 二次登录 Token | localStorage | `cdp-preLoginToken` | user |
| 动态菜单 | localStorage | `cdp-menu` | permission |
| 主题设置 | localStorage | `theme` / `layout` / `themeColor` | settings |
| 侧边栏状态 | localStorage | `sidebarStatus` | app |
| 语言 | localStorage | `language` | app |

---

## 五、新增 Store 标准写法

```typescript
// src/cdp-admin/store/modules/example.ts
import { defineStore } from "pinia";
import { store } from "@/cdp-admin/store";
import { useStorage } from "@vueuse/core";

export const useExampleStore = defineStore("example", () => {
  // State
  const count = ref(0);
  const name = useStorage("cdp-example-name", ""); // 需持久化的用 useStorage

  // Actions
  function increment() {
    count.value++;
  }

  async function fetchData() {
    const { data } = await someApi();
    // 处理数据...
  }

  return { count, name, increment, fetchData };
});

// 组件外调用的 Hook 函数
export function useExampleStoreHook() {
  return useExampleStore(store);
}
```

---

## 六、常见陷阱

1. **组件外 Store 调用**: 必须用 `useXxxStoreHook()`，直接 `useXxxStore()` 会因 Pinia 未初始化报错
2. **Token 双模式**: `globalConfig.sessionCookie === true` 时 token 是普通字符串（非响应式），赋值用 `token = value`；`false` 时是 `useStorage` 返回的 Ref，赋值用 `token.value = value`
3. **userInfo 缓存**: `getUserInfo()` 优先从 localStorage 恢复，后端更新用户信息后前端需要清除 `cdp-userInfo` 才能刷新
4. **menu 缓存**: `generateRoutes()` 优先从 localStorage 恢复菜单数据。当前代码中 `menu.value = asyncRoutes` 被注释，即未缓存菜单（每次刷新都请求接口）
5. **login 多状态码**: `login()` 返回的 Promise resolve 并不一定代表登录成功，需要检查 response.data.code（10010/10020/10030 为特殊状态）
