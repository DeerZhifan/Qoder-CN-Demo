---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/03-module-state-management.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-state-management.md
  - docs/knowledge-base/5-代码片段库/pinia-store-standard.ts
---

# CDP 状态管理开发规范

## 适用场景

当需要新增 Pinia Store、管理跨组件共享状态、使用持久化数据、在非 setup 上下文中访问 Store 时，必须遵循本规则。

## 前置依赖

- Pinia 2.1 + `@vueuse/core`（`useStorage`）
- Store 统一存放在 `src/cdp-admin/store/modules/`
- Pinia 实例：`src/cdp-admin/store/index.ts` 导出 `store`
- 路径别名 `@/` 映射到 `src/`

## 配置要点

### 5 个核心 Store

| Store | 文件 | 职责 |
|-------|------|------|
| `useUserStore` | `store/modules/user.ts` | 登录/登出、Token 管理、用户信息（角色、权限） |
| `usePermissionStore` | `store/modules/permission.ts` | 动态路由生成、菜单缓存 |
| `useSettingsStore` | `store/modules/settings.ts` | UI 配置（主题色、布局、暗黑模式、水印） |
| `useAppStore` | `store/modules/app.ts` | 全局应用状态（侧边栏折叠、语言、设备类型） |
| `useTagsViewStore` | `store/modules/tagsView.ts` | 已打开标签页追踪、keep-alive 缓存 |

### Store 定义风格

必须使用 Composition API 风格（Setup Store），不使用 Options API 风格：

```typescript
// 正确：Setup Store
export const useExampleStore = defineStore("example", () => {
  const count = ref(0);
  const doubled = computed(() => count.value * 2);
  function increment() { count.value++; }
  return { count, doubled, increment };
});

// 错误：Options API
export const useExampleStore = defineStore("example", {
  state: () => ({ count: 0 }),  // 不要使用
});
```

### 持久化方案

使用 `@vueuse/core` 的 `useStorage` 实现 localStorage 持久化，key 统一加 `cdp-` 前缀：

```typescript
import { useStorage } from "@vueuse/core";

// 持久化到 localStorage（页面刷新后自动恢复）
const cachedConfig = useStorage("cdp-example-config", { pageSize: 20 });
// 普通响应式数据（刷新后丢失）
const loading = ref(false);
```

已使用的 localStorage key：

| Key | Store | 用途 |
|-----|-------|------|
| `cdp-token` | user | 登录 Token（非 Cookie 模式） |
| `cdp-userInfo` | user | 用户信息缓存 |
| `cdp-menu` | permission | 动态菜单缓存 |
| `cdp-preLoginToken` | user | 二次登录临时 Token |

### Hook 模式（组件外访问）

在路由守卫、Axios 拦截器、工具函数等非 setup 上下文中，必须通过 Hook 模式访问 Store：

```typescript
import { store } from "@/cdp-admin/store";

export function useExampleStoreHook() {
  return useExampleStore(store);
}
```

## 代码模式

### 推荐写法

#### 标准 Store 定义

```typescript
// src/cdp-admin/store/modules/example.ts
import { defineStore } from "pinia";
import { store } from "@/cdp-admin/store";
import { useStorage } from "@vueuse/core";

export const useExampleStore = defineStore("example", () => {
  // ========== State ==========
  const loading = ref(false);
  const list = ref<ExampleItem[]>([]);

  // 持久化数据
  const cachedFilter = useStorage("cdp-example-filter", "");

  // ========== Getters（使用 computed） ==========
  const isEmpty = computed(() => list.value.length === 0);
  const count = computed(() => list.value.length);

  // ========== Actions ==========
  async function fetchList() {
    loading.value = true;
    try {
      const { data } = await listExamplePages({});
      list.value = data.records;
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    list.value = [];
    cachedFilter.value = "";
  }

  // ========== Return ==========
  return { loading, list, cachedFilter, isEmpty, count, fetchList, reset };
});

// 组件外调用的 Hook 函数
export function useExampleStoreHook() {
  return useExampleStore(store);
}
```

#### 组件内使用 Store

```vue
<script setup lang="ts">
import { useExampleStore } from "@/cdp-admin/store/modules/example";

const exampleStore = useExampleStore();

// 直接访问
console.log(exampleStore.count);
exampleStore.fetchList();

// 解构响应式属性必须用 storeToRefs
const { count, loading } = storeToRefs(exampleStore);
// 解构方法不需要 storeToRefs
const { fetchList, reset } = exampleStore;
</script>
```

#### 组件外使用 Store（路由守卫、拦截器）

```typescript
// src/permission.ts
import { useUserStoreHook } from "@/cdp-admin/store/modules/user";
import { usePermissionStoreHook } from "@/cdp-admin/store/modules/permission";

const userStore = useUserStoreHook();
const permissionStore = usePermissionStoreHook();
```

### 禁止事项

1. **禁止使用 Options API 风格定义 Store** — 必须使用 `defineStore("id", () => {...})` Setup Store 写法
2. **禁止在非 setup 上下文中直接调用 `useXxxStore()`** — 必须通过 `useXxxStoreHook()` 传入 Pinia 实例
3. **禁止解构 Store 响应式属性不使用 `storeToRefs()`** — 直接解构会丢失响应式
4. **禁止使用第三方持久化插件** — 统一使用 `@vueuse/core` 的 `useStorage`
5. **禁止 `useStorage` key 不加 `cdp-` 前缀** — 避免与其他应用冲突
6. **禁止在 Store 中直接操作 DOM** — Store 层管理状态，不处理 UI
7. **禁止使用 any 类型** — State 和 Action 参数/返回值必须定义明确的类型
8. **禁止 Store 间循环依赖** — 如果 Store A 依赖 Store B，Store B 不可反向依赖 Store A
