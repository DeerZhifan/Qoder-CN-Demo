# 如何使用样式系统

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: SCSS, 变量, mixin, UnoCSS, 主题切换, 暗黑模式, 样式, variables.scss, theme

---

## 概述

CDP Web 样式系统由三部分组成：SCSS 全局变量/mixin（`src/styles/`）、UnoCSS 原子化工具类、Element Plus 主题定制。Vite 通过 `additionalData` 自动注入全局 SCSS 变量和 mixin，组件中无需手动导入。

## 使用全局 SCSS 变量

全局变量定义在 `src/styles/variables.scss`，Vite 自动注入到所有 Vue 组件的 `<style lang="scss">` 中。

### 常用变量

```scss
// 布局尺寸
$navBarHeight: 64px;    // 顶部导航栏高度
$tagviewHeight: 34px;   // 标签栏高度
$sidebarWidth: 237px;   // 侧边栏宽度

// 颜色
$c-m: #3c78b7;          // 主色
$c-red: #e64a42;        // 红色
$c-green: #39b76b;      // 绿色
$c-orange: #ee8c0f;     // 橙色
$c-font: #333333;       // 字体黑
$c-font-grey: #808080;  // 字体灰
$c-border: #ccc;        // 边框色
$c-page-bg: #f5f5f5;    // 页面背景

// 层级
$z-top-max: 999999;     // 绝对顶层
$z-top-front: 100;      // 顶层
```

### 在组件中使用

```vue
<style lang="scss" scoped>
.header {
  height: $navBarHeight;         // 直接使用，无需 @import
  color: $c-font;
  background-color: $c-page-bg;
}
</style>
```

## 使用全局 Mixin

全局 mixin 定义在 `src/styles/mixin.scss`，同样自动注入。

### 常用 mixin

```scss
// 文本溢出省略（支持多行）
@include ellipsis(1);      // 单行省略
@include ellipsis(2);      // 两行省略

// 背景图片
@include bg("icons/arrow.png");

// 居中背景图片（指定宽高）
@include bgCenter("logo.png", 200px, 100px);

// 绝对居中
@include center();                    // 水平垂直居中
@include center(200px, 100px);       // 指定宽高居中

// 圆形
@include circle(40px);

// 清除浮动
@include clearfix;

// 自定义滚动条
@include scrollBar;

// 三角形
@include triangle(10px, 8px, #333, down);  // 方向: up/right/down/left
```

### 在组件中使用

```vue
<style lang="scss" scoped>
.title {
  @include ellipsis(1);    // 单行省略
}

.avatar {
  @include circle(48px);   // 48px 圆形
}
</style>
```

## 使用 UnoCSS 原子化类

UnoCSS 已在 `vite.config.ts` 中配置，在模板中直接使用原子化 class：

```vue
<template>
  <div class="flex items-center justify-between p-4">
    <span class="text-lg font-bold text-gray-800">标题</span>
    <el-button class="ml-2">操作</el-button>
  </div>

  <!-- 常用 UnoCSS 类 -->
  <div class="w-full h-200px">全宽，固定高度</div>
  <div class="mt-4 mb-2 px-3">外间距、内间距</div>
  <div class="rounded-md border border-gray-200">圆角+边框</div>
</template>
```

## 主题切换

通过 Settings Store 控制主题，支持 `light`、`dark`、`blue` 三种主题：

```typescript
import { useSettingsStore } from "@/cdp-admin/store/modules/settings";

const settingsStore = useSettingsStore();

// 切换暗黑模式
settingsStore.changeSetting({ key: "theme", value: "dark" });

// 切换回亮色
settingsStore.changeSetting({ key: "theme", value: "light" });
```

主题通过在 `<html>` 元素上添加 `theme-dark` / `theme-light` / `theme-blue` 类名实现。暗黑模式的变量覆盖在 `src/styles/dark.scss` 中定义。

## CSS 变量（主题色）

侧边栏等组件颜色通过 CSS 变量控制，定义在 `variables.scss` 的 `:root` 中：

```scss
:root {
  --menuBg: #304156;
  --menuText: #bfcbd9;
  --menuActiveText: #409eff;
  --menuHover: #263445;
  --subMenuBg: #1f2d3d;
}
```

## 注意事项

> 注意：全局变量和 mixin 由 Vite `additionalData` 自动注入，不要在组件中手动 `@import` 或 `@use` `variables.scss` 或 `mixin.scss`，否则会重复导入。

> 注意：覆盖 Element Plus 组件样式时，在 `<style scoped>` 中需使用 `:deep()` 穿透选择器，如 `:deep(.el-input__inner) { color: red; }`。

> 注意：Element Plus 主题定制在 `src/styles/theme.scss` 中通过覆盖 CSS 变量实现，不要直接修改 `node_modules` 中的源码。

> 注意：UnoCSS 和 SCSS 可混合使用。布局用 UnoCSS 原子类，复杂样式用 SCSS。
