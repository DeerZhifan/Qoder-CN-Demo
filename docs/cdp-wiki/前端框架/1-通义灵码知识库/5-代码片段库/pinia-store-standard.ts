/**
 * [场景:新增Pinia Store] — defineStore 组合式写法 + Hook 模式
 *
 * 源码参考: src/cdp-admin/store/modules/user.ts
 */

import { defineStore } from "pinia";
import { store } from "@/cdp-admin/store";
import { useStorage } from "@vueuse/core";

export const useExampleStore = defineStore("example", () => {
  // ==================== State ====================

  // 普通响应式数据（刷新后丢失）
  const loading = ref(false);
  const list = ref<any[]>([]);

  // 持久化数据（存储到 localStorage，刷新后恢复）
  const cachedFilter = useStorage("cdp-example-filter", "");

  // ==================== Getters（使用 computed） ====================

  const isEmpty = computed(() => list.value.length === 0);
  const count = computed(() => list.value.length);

  // ==================== Actions ====================

  async function fetchList() {
    loading.value = true;
    try {
      // const { data } = await someApi();
      // list.value = data;
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    list.value = [];
    cachedFilter.value = "";
  }

  // ==================== Return ====================

  return {
    loading,
    list,
    cachedFilter,
    isEmpty,
    count,
    fetchList,
    reset,
  };
});

/**
 * 组件外调用的 Hook 函数
 *
 * 使用场景: 路由守卫、Axios 拦截器、工具函数等非组件环境
 *
 * @example
 * // ❌ 组件外直接调用会报错
 * const store = useExampleStore(); // Pinia 未初始化
 *
 * // ✅ 使用 Hook 模式
 * import { useExampleStoreHook } from "@/store/modules/example";
 * const store = useExampleStoreHook();
 */
export function useExampleStoreHook() {
  return useExampleStore(store);
}
