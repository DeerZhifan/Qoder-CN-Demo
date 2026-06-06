---
type: gen
description: 基于 CDP 前端框架规范，生成 Pinia Store 模块
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/03-module-state-management.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-state-management.md
  - docs/knowledge-base/5-代码片段库/pinia-store-standard.ts
---

# 生成 Pinia Store 模块

## 输入参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| Store 名称 | 是 | camelCase，不含 use/Store 前后缀 | `projectConfig` |
| 状态字段列表 | 否 | 字段名、类型、默认值 | `loading: boolean = false, list: ProjectItem[] = []` |
| 是否需要持久化 | 否 | 指定需要持久化到 localStorage 的字段（默认否） | `cachedFilter, selectedId` |

## 执行步骤

### 第 1 步：创建 Store 文件

生成文件路径：`src/cdp-admin/store/modules/{Store名称}.ts`

```typescript
import { defineStore } from "pinia";
import { store } from "@/cdp-admin/store";
import { useStorage } from "@vueuse/core";

export const use{Store名称PascalCase}Store = defineStore("{Store名称}", () => {
  // ==================== State ====================

  // 普通响应式数据（刷新后丢失）
  // {遍历状态字段列表中不需要持久化的字段}
  const loading = ref(false);
  const list = ref<any[]>([]);

  // 持久化数据（存储到 localStorage，刷新后恢复）
  // {遍历状态字段列表中需要持久化的字段}
  // const {字段名} = useStorage("cdp-{Store名称}-{字段名}", {默认值});

  // ==================== Getters（使用 computed） ====================

  const isEmpty = computed(() => list.value.length === 0);
  const count = computed(() => list.value.length);

  // ==================== Actions ====================

  async function fetchList() {
    loading.value = true;
    try {
      // TODO: 替换为实际的 API 调用
      // const { data } = await someApi();
      // list.value = data.records;
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    loading.value = false;
    list.value = [];
    // TODO: 重置持久化字段
  }

  // ==================== Return ====================

  return {
    // state
    loading,
    list,
    // getters
    isEmpty,
    count,
    // actions
    fetchList,
    reset,
  };
});

/**
 * 组件外调用的 Hook 函数
 *
 * 使用场景: 路由守卫、Axios 拦截器、工具函数等非 setup 上下文
 *
 * @example
 * import { use{Store名称PascalCase}StoreHook } from "@/cdp-admin/store/modules/{Store名称}";
 * const store = use{Store名称PascalCase}StoreHook();
 */
export function use{Store名称PascalCase}StoreHook() {
  return use{Store名称PascalCase}Store(store);
}
```

### 第 2 步：定义状态字段

根据输入的状态字段列表，替换模板中的 State 区域：

- 普通响应式字段使用 `ref()` 或 `reactive()`
- 需要持久化的字段使用 `useStorage("cdp-{Store名称}-{字段名}", 默认值)`
- Getter 使用 `computed()`

### 第 3 步：配置持久化（如需要）

如果指定了需要持久化的字段，将对应字段改为 `useStorage` 声明：

```typescript
// 持久化前
const selectedId = ref("");

// 持久化后
const selectedId = useStorage("cdp-{Store名称}-selectedId", "");
```

注意：`useStorage` 的 key 统一使用 `cdp-` 前缀，格式为 `cdp-{Store名称}-{字段名}`。

## 完成后提醒

生成完成后，请检查以下事项：

1. **补充状态字段** — 替换模板中的 `TODO` 注释，填写实际的业务状态字段和类型定义
2. **替换 any 类型** — 将 `ref<any[]>([])` 等临时类型替换为具体的业务接口类型
3. **补充 Actions** — 根据业务需要添加异步数据获取、状态更新等方法
4. **添加 Getters** — 根据需要添加 `computed` 计算属性
5. **确认持久化 key 唯一** — 检查 `useStorage` 的 key 不与已有 Store 冲突
6. **组件外访问** — 如果需要在路由守卫或拦截器中使用，确保通过 `useXxxStoreHook()` 调用
7. **解构注意** — 组件中解构响应式属性时使用 `storeToRefs()`，解构方法则直接解构
