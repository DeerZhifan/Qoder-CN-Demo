---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/06-module-style-system.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-style.md
  - docs/knowledge-base/5-代码片段库/scss-variables-usage.vue
---

# CDP 样式系统开发规范
## 适用场景

本规则适用于 CDP Web 前端框架下的样式相关开发任务，包括：编写组件样式、使用全局 SCSS 变量与 mixin、主题切换与暗黑模式适配、UnoCSS 原子化类使用、Element Plus 主题定制、响应式布局实现。

## 前置依赖

- 全局 SCSS 变量（`src/styles/variables.scss`）和 mixin（`src/styles/mixin.scss`）通过 Vite `additionalData` 自动注入，无需手动 `@import`
- Element Plus 暗黑模式 CSS 变量已在 `main.ts` 中导入（`element-plus/theme-chalk/dark/css-vars.css`）
- UnoCSS 已在 `vite.config.ts` 中配置，支持 `presetUno`、`presetAttributify`、`presetIcons`
- 主题定义文件：`src/styles/theme.scss`（light/dark/blue 三套主题）
- 暗黑模式补充覆盖：`src/styles/dark.scss`

## 配置要点

### SCSS 变量体系

```scss
// 布局尺寸（直接在 <style lang="scss"> 中使用）
$navBarHeight: 64px;    // 顶部导航栏高度
$tagviewHeight: 34px;   // 标签栏高度
$sidebarWidth: 237px;   // 侧边栏宽度

// 颜色
$c-m: #3c78b7;          // 平台主色
$c-font: #333333;       // 字体色
$c-font-grey: #808080;  // 次要文字
$c-border: #ccc;        // 边框色
$c-page-bg: #f5f5f5;    // 页面背景

// 层级
$z-top-max: 999999;     // 绝对顶层
```

### CSS 自定义属性（主题色）

```scss
// 定义在 variables.scss :root 中，随主题类名变化
:root {
  --menuBg: #304156;
  --menuText: #bfcbd9;
  --menuActiveText: #409eff;
  --menuHover: #263445;
}
```

### 样式导入顺序（main.ts）

```typescript
import "element-plus/dist/index.css";                // 1. Element Plus 基础
import "element-plus/theme-chalk/dark/css-vars.css";  // 2. 暗黑模式变量
import "@/styles/index.scss";                         // 3. 项目全局样式
import "uno.css";                                     // 4. UnoCSS（最后，可覆盖）
```

## 代码模式

### 推荐写法

#### 使用全局 SCSS 变量

```vue
<style scoped lang="scss">
.header {
  height: $navBarHeight;
  color: $c-font;
  background-color: $c-page-bg;
}

.content {
  display: flex;
  height: calc(100vh - #{$navBarHeight} - #{$tagviewHeight});
}

.sidebar {
  width: $sidebarWidth;
  background: var(--menuBg);    /* 主题相关用 CSS 变量 */
  color: var(--menuText);
}
</style>
```

#### 在 TypeScript 中使用 SCSS 变量

```typescript
import variables from "@/styles/variables.module.scss";
const navHeight = variables.navBarHeight;   // "64px"
const sidebarW = variables.sidebarWidth;    // "237px"
```

#### 使用全局 Mixin

```vue
<style scoped lang="scss">
.title { @include ellipsis(1); }       // 单行省略
.desc  { @include ellipsis(2); }       // 两行省略
.avatar { @include circle(48px); }     // 圆形元素
.panel { @include scrollBar; }         // 自定义滚动条
</style>
```

#### BEM 命名 + SCSS 嵌套

```vue
<style scoped lang="scss">
.user-card {
  padding: 16px;
  &__header { display: flex; align-items: center; }
  &__title  { @include ellipsis(1); color: $c-font; }
  &__body   { margin-top: 12px; }
  &--active { border-color: $c-m; }
}
</style>
```

#### 主题切换

```typescript
import { useSettingsStore } from "@/cdp-admin/store/modules/settings";
const settingsStore = useSettingsStore();

// 切换主题（light / dark / blue）
settingsStore.changeSetting({ key: "theme", value: "dark" });
```

#### Element Plus 主题覆盖

```scss
// src/styles/theme.scss — 通过覆盖 CSS 变量定制
.theme-dark {
  --menuBg: rgba(0, 0, 0, 0.8);
  --menuText: rgba(255, 255, 255, 0.7);
}
.theme-blue {
  --menuBg: #1d3a5f;
  --menuText: #ffffff;
}
```

#### Element Plus 样式穿透

```vue
<style scoped lang="scss">
:deep(.el-input__inner) {
  color: $c-font;
}
:deep(.el-table__header) {
  background: $c-page-bg;
}
</style>
```

#### UnoCSS 原子化类

```vue
<template>
  <div class="flex items-center justify-between p-4">
    <span class="text-lg font-bold text-gray-800">标题</span>
    <el-button class="ml-2">操作</el-button>
  </div>
  <div class="w-full h-200px mt-4 rounded-md border border-gray-200">内容</div>
</template>
```

#### 响应式布局（UnoCSS + SCSS 混合）

```vue
<template>
  <div class="page-container flex flex-col h-full">
    <div class="flex-shrink-0 p-4">顶部工具栏</div>
    <div class="flex-1 overflow-auto p-4">
      <div class="content-area">主内容</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.content-area {
  min-height: calc(100vh - #{$navBarHeight} - #{$tagviewHeight} - 80px);
}
</style>
```

### 禁止事项

1. **禁止手动 @import 或 @use 全局 SCSS 文件** — `variables.scss` 和 `mixin.scss` 已由 Vite `additionalData` 自动注入
2. **禁止直接修改 node_modules 中的 Element Plus 样式** — 在 `src/styles/theme.scss` 中通过 CSS 变量覆盖
3. **禁止在非 scoped 样式中写业务样式** — 必须使用 `<style scoped lang="scss">`，避免全局污染
4. **禁止混淆 SCSS 变量和 CSS 自定义属性** — 布局尺寸用 SCSS 变量，主题颜色用 CSS 自定义属性
5. **禁止在 calc() 中直接使用 SCSS 变量** — 必须用插值语法 `#{$var}`
6. **禁止调整 main.ts 中的样式导入顺序** — 顺序为 Element Plus > 暗黑变量 > 项目样式 > UnoCSS
7. **禁止在 scoped 样式中不使用 :deep() 就覆盖子组件样式** — 必须使用 `:deep()` 选择器穿透
