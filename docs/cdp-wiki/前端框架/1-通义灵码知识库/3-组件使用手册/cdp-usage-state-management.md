# 如何使用 Pinia 状态管理

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: Pinia, Store, 状态管理, useStorage, 持久化, defineStore, useUserStore, usePermissionStore

---

## 概述

CDP Web 使用 Pinia（Composition API 风格）进行状态管理，核心 Store 位于 `src/cdp-admin/store/modules/`。通过 `@vueuse/core` 的 `useStorage` 实现 localStorage 持久化，通过 Hook 模式支持在非 setup 上下文中访问 Store。

## 5 个核心 Store

| Store | 文件 | 职责 |
|-------|------|------|
| `useUserStore` | `store/modules/user.ts` | 登录/登出、Token 管理、用户信息（角色、权限） |
| `usePermissionStore` | `store/modules/permission.ts` | 动态路由生成、菜单缓存 |
| `useSettingsStore` | `store/modules/settings.ts` | UI 配置（主题色、布局、暗黑模式、水印） |
| `useAppStore` | `store/modules/app.ts` | 全局应用状态（侧边栏折叠等） |
| `useTagsViewStore` | `store/modules/tagsView.ts` | 已打开标签页追踪 |

## 新增 Store 的标准步骤

### 第 1 步：创建 Store 文件

```typescript
// src/cdp-admin/store/modules/example.ts
import { defineStore } from "pinia";
import { useStorage } from "@vueuse/core";
import { store } from "@/cdp-admin/store";

export const useExampleStore = defineStore("example", () => {
  // 普通响应式状态
  const count = ref(0);
  const loading = ref(false);

  // 持久化到 localStorage（页面刷新不丢失）
  const cachedConfig = useStorage("cdp-example-config", {
    pageSize: 20,
    showAdvanced: false,
  });

  // actions
  function increment() {
    count.value++;
  }

  async function fetchData() {
    loading.value = true;
    try {
      // 调用 API...
    } finally {
      loading.value = false;
    }
  }

  return {
    count,
    loading,
    cachedConfig,
    increment,
    fetchData,
  };
});

// Hook 模式：在非 setup 上下文中使用（路由守卫、拦截器等）
export function useExampleStoreHook() {
  return useExampleStore(store);
}
```

### 第 2 步：在组件中使用

```vue
<script setup lang="ts">
import { useExampleStore } from "@/cdp-admin/store/modules/example";

const exampleStore = useExampleStore();

// 直接访问
console.log(exampleStore.count);
exampleStore.increment();

// 解构需用 storeToRefs 保持响应式
const { count, loading } = storeToRefs(exampleStore);
</script>
```

## 在非 setup 上下文中使用 Store

路由守卫、Axios 拦截器等非组件环境中，不能直接调用 `useXxxStore()`，必须使用 Hook 模式：

```typescript
// src/permission.ts 中的用法
import { useUserStoreHook } from "@/cdp-admin/store/modules/user";
import { usePermissionStoreHook } from "@/cdp-admin/store/modules/permission";

const userStore = useUserStoreHook();       // 传入 store 实例
const permissionStore = usePermissionStoreHook();
```

> 注意：Hook 函数内部调用 `useXxxStore(store)`，其中 `store` 是从 `@/cdp-admin/store` 导入的 Pinia 实例。这是在 `setup()` 之外使用 Store 的标准方式。

## useStorage 持久化

`useStorage` 自动将状态同步到 localStorage，页面刷新后自动恢复：

```typescript
// key: localStorage 中的 key 名
// 第二个参数：默认值（首次使用时的初始值）
const userInfo = useStorage("cdp-userInfo", <any>{});
const token = useStorage("cdp-token", "");
const menu = useStorage("cdp-menu", <any[]>[]);
```

框架已使用的 localStorage key：

| Key | Store | 用途 |
|-----|-------|------|
| `cdp-token` | user | 登录 Token（非 Cookie 模式时） |
| `cdp-userInfo` | user | 用户信息缓存 |
| `cdp-menu` | permission | 动态菜单缓存 |
| `cdp-preLoginToken` | user | 二次登录临时 Token |

## 注意事项

> 注意：必须使用 Composition API 风格（`defineStore("id", () => {...})`）定义 Store，不使用 Options API 风格。

> 注意：解构 Store 中的响应式属性时，必须使用 `storeToRefs()`，否则会丢失响应式。直接解构 actions（方法）则不需要。

> 注意：在非 setup 上下文中，必须通过 `useXxxStoreHook()` 访问 Store，不能直接调用 `useXxxStore()`。

> 注意：`useStorage` 的 key 命名统一使用 `cdp-` 前缀，避免与其他应用冲突。
