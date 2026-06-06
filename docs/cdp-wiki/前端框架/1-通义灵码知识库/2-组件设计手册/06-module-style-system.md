# 样式系统设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: SCSS, 变量, 主题, 暗黑模式, mixin, UnoCSS, CSS变量, 换肤

---

## 一、样式文件结构

源码位置：`src/styles/`

| 文件 | 职责 | 大小 |
|------|------|------|
| `index.scss` | 入口文件，聚合所有样式 | 聚合 |
| `variables.scss` | SCSS 变量 + CSS 自定义属性 + `:export` 导出到 JS | 核心 |
| `variables.module.scss` | SCSS 模块导出（供 TypeScript import） | 桥接 |
| `mixin.scss` | 12+ 通用 mixin | 工具 |
| `theme.scss` | 多主题定义（light/dark/blue） | 主题 |
| `dark.scss` | 暗黑模式补充覆盖 | 主题 |
| `cdp.scss` | 平台级全局样式 | 业务 |
| `sidebar.scss` | 侧边栏专用样式 | 布局 |
| `reset.scss` | CSS 重置 | 基础 |
| `transition.scss` | 动画/过渡定义 | 动画 |

---

## 二、全局变量注入机制

### Vite 自动注入

```typescript
// vite.config.ts
css: {
  preprocessorOptions: {
    scss: {
      additionalData: `
        @use "@/styles/variables.scss" as *;
        @use "@/styles/mixin.scss" as *;
      `,
    },
  },
}
```

**效果:** 所有 `.vue` 和 `.scss` 文件自动可用全局 SCSS 变量和 mixin，**无需手动 import**。

### 关键 SCSS 变量

```scss
// 布局尺寸
$navBarHeight: 64px;        // 顶部导航栏高度
$tagviewHeight: 34px;       // 标签栏高度
$sidebarWidth: 237px;       // 侧边栏宽度

// 颜色
$c-m: #3c78b7;              // 平台主色
$c-theme-change: #409EFF;   // Element Plus 默认主题色
$c-red: #f40;               // 错误/危险
$c-green: #00bf00;          // 成功
$c-orange: #f60;            // 警告
$c-grey: #999;              // 次要文本
```

### CSS 自定义属性（主题相关）

```scss
:root {
  --menuBg: #304156;        // 菜单背景色
  --menuText: #bfcbd9;      // 菜单文字色
  --menuActiveText: #409eff; // 菜单激活文字色
  --menuHover: #263445;     // 菜单悬停背景色
}
```

### SCSS 变量导出到 JS

```scss
// variables.scss
:export {
  navBarHeight: $navBarHeight;
  tagviewHeight: $tagviewHeight;
  sidebarWidth: $sidebarWidth;
}
```

```typescript
// 在 TypeScript 中使用
import variables from "@/styles/variables.module.scss";
console.log(variables.navBarHeight); // "64px"
```

---

## 三、常用 Mixin

源码位置：`src/styles/mixin.scss`

### 文本截断

```scss
// 单行省略
@mixin ellipsis() {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 多行省略
@mixin ellipsis($lines) {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: $lines;
  -webkit-box-orient: vertical;
}

// 使用
.title { @include ellipsis; }           // 单行
.description { @include ellipsis(2); }  // 两行
```

### 居中定位

```scss
// 水平垂直居中
@mixin center($width, $height) {
  position: absolute;
  left: 50%;
  top: 50%;
  width: $width;
  height: $height;
  margin-left: -($width / 2);
  margin-top: -($height / 2);
}

// 仅水平居中
@mixin center-x($width) { ... }

// 仅垂直居中
@mixin center-y($height) { ... }
```

### 自定义滚动条

```scss
@mixin scrollBar {
  &::-webkit-scrollbar { width: 6px; height: 6px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(144, 147, 153, 0.3);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-track { background: transparent; }
}

// 使用
.content { @include scrollBar; }
```

### 其他 Mixin

```scss
@mixin bg($url)                    // 背景图
@mixin circle($size)               // 圆形元素
@mixin clearfix                    // 清除浮动
@mixin triangle($direction, $size, $color)  // CSS 三角形
```

---

## 四、主题切换

### 三套主题

| 主题 | 类名 | 菜单背景 | 菜单文字 |
|------|------|---------|---------|
| Light | `theme-light` | #304156 | #bfcbd9 |
| Dark | `theme-dark` | 暗色叠加 | 浅灰色 |
| Blue | `theme-blue` | 深蓝色 | 白色 |

### 切换原理

```typescript
// settings store
function changeSetting({ key, value }) {
  // 应用主题类名到根元素
  document.documentElement.className = `theme-${value}`;
}
```

### CSS 实现

```scss
// theme.scss
:root {
  --menuBg: #304156;
  --menuText: #bfcbd9;
  --menuActiveText: #409eff;
}

.theme-dark {
  --menuBg: rgba(0, 0, 0, 0.8);
  --menuText: rgba(255, 255, 255, 0.7);
}

.theme-blue {
  --menuBg: #1d3a5f;
  --menuText: #ffffff;
}
```

### Element Plus 暗黑模式

```typescript
// main.ts
import "element-plus/theme-chalk/dark/css-vars.css";  // 导入暗黑 CSS 变量
```

暗黑模式通过 `html.dark` 类名触发 Element Plus 暗黑样式。

---

## 五、UnoCSS 配置

源码位置：`uno.config.ts`

### 预设

- `presetUno` — 原子化 CSS 基础（类似 Tailwind）
- `presetAttributify` — 属性化模式：`<div bg="blue-400" text="sm" />`
- `presetIcons` — 图标系统
- `presetTypography` — 排版工具

### 常用 Shortcuts

```typescript
shortcuts: {
  "flex-center": "flex justify-center items-center",
}
```

### 主题色对接

```typescript
theme: {
  colors: {
    primary: "var(--el-color-primary)",
  },
}
```

### 在组件中使用

```vue
<template>
  <!-- 原子化类名 -->
  <div class="flex-center p-4 text-sm text-gray-600">
    内容
  </div>

  <!-- 属性化模式 -->
  <div flex items-center p-4>
    内容
  </div>
</template>
```

### 样式导入顺序

```typescript
// main.ts — 导入顺序很重要
import "element-plus/dist/index.css";           // 1. Element Plus 基础
import "element-plus/theme-chalk/dark/css-vars.css"; // 2. 暗黑模式变量
import "@/styles/index.scss";                    // 3. 项目全局样式
import "uno.css";                                // 4. UnoCSS（最后，可覆盖）
```

---

## 六、常见陷阱

1. **SCSS 变量不需要 import**: Vite `additionalData` 已自动注入 `variables.scss` 和 `mixin.scss`
2. **CSS 自定义属性 vs SCSS 变量**: 主题相关的颜色用 CSS 自定义属性（`var(--menuBg)`），布局尺寸用 SCSS 变量（`$navBarHeight`）
3. **暗黑模式不完整**: 如果某些组件在暗黑模式下样式异常，需在 `theme.scss` 或 `dark.scss` 中补充覆盖
4. **UnoCSS 不生效**: 确认 `main.ts` 中已导入 `"uno.css"`，且位于样式导入链最后
5. **`:export` 用于 JS**: SCSS 的 `:export` 块将变量导出到 JavaScript，通过 `import` 模块方式使用
