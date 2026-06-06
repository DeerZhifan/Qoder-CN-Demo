<!--
  [场景:新增NUI封装组件] — 完整的 NUI 组件开发模板
  放置位置: src/cdp-common-nui/packages/组件名/index.vue

  注意: 必须声明 name 属性，否则不会被全局注册！
  注册机制: main.ts 中 import.meta.glob 读取 name → app.component(name, component)
-->

<!-- 必须：单独的 script 块声明 name -->
<script lang="ts">
export default {
  name: "NuiMyComponent", // ← 全局注册名，必须唯一
};
</script>

<script setup lang="ts">
// Props 定义
const props = withDefaults(
  defineProps<{
    /** 标题 */
    title?: string;
    /** 是否显示边框 */
    bordered?: boolean;
    /** 尺寸 */
    size?: "small" | "default" | "large";
  }>(),
  {
    title: "",
    bordered: true,
    size: "default",
  }
);

// Emits 定义
const emit = defineEmits<{
  (e: "click"): void;
  (e: "change", value: string): void;
}>();

// 组件逻辑
const handleClick = () => {
  emit("click");
};
</script>

<template>
  <div
    class="nui-my-component"
    :class="[
      `nui-my-component--${size}`,
      { 'is-bordered': bordered },
    ]"
    @click="handleClick"
  >
    <div v-if="title" class="nui-my-component__header">
      {{ title }}
    </div>
    <div class="nui-my-component__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="nui-my-component__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped lang="scss">
// 全局 SCSS 变量已自动注入，可直接使用
.nui-my-component {
  border-radius: 4px;
  background: #fff;

  &.is-bordered {
    border: 1px solid #ebeef5;
  }

  &--small {
    padding: 8px;
  }

  &--default {
    padding: 16px;
  }

  &--large {
    padding: 24px;
  }

  &__header {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
    color: $c-m; // 使用全局变量
  }

  &__footer {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }
}
</style>
