<!--
  [场景:使用全局SCSS变量] — 在组件中使用全局 SCSS 变量和导出到 JS
  全局变量已通过 Vite additionalData 自动注入，无需手动 import
-->
<script setup lang="ts">
// 在 TypeScript 中使用 SCSS 变量（通过 :export 导出）
import variables from "@/styles/variables.module.scss";

const navHeight = variables.navBarHeight;   // "64px"
const sidebarW = variables.sidebarWidth;    // "237px"
const tagHeight = variables.tagviewHeight;  // "34px"
</script>

<template>
  <div class="example-page">
    <!-- 使用全局 SCSS 变量计算布局 -->
    <div class="header">顶部导航（高度使用 $navBarHeight）</div>
    <div class="content">
      <div class="sidebar">侧边栏（宽度使用 $sidebarWidth）</div>
      <div class="main">主内容区</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// ✅ 直接使用全局变量，不需要 @import 或 @use
.header {
  height: $navBarHeight;        // 64px
  line-height: $navBarHeight;
  background: $c-m;             // 主色 #3c78b7
  color: #fff;
}

.content {
  display: flex;
  // calc 中使用 SCSS 变量
  height: calc(100vh - #{$navBarHeight} - #{$tagviewHeight});
}

.sidebar {
  width: $sidebarWidth;         // 237px
  background: var(--menuBg);    // CSS 自定义属性（主题相关）
  color: var(--menuText);
}

.main {
  flex: 1;
  padding: 16px;
}

// 使用主题相关的 CSS 自定义属性
.theme-aware {
  background: var(--menuBg);
  color: var(--menuActiveText);
}
</style>
