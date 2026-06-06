---
type: gen
description: 基于 CDP 前端框架规范，生成主题配置文件
---

# 生成主题配置

## 描述

基于 CDP 前端框架规范，生成完整的主题配置文件，包括 SCSS 变量定义、Element Plus 主题覆盖和主题切换逻辑。生成的主题将无缝集成到现有的 light/dark/blue 主题体系中。

## 输入参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| 主题名称 | 是 | 主题的英文标识符（kebab-case） | `green`、`purple`、`custom-brand` |
| 主色调 | 是 | 主题主色调（十六进制色值） | `#00b894`、`#6c5ce7` |
| 是否支持深色模式 | 否 | 是否生成暗黑模式变体（默认：是） | `是` / `否` |

## 步骤

### 步骤 1：创建 SCSS 变量文件

在 `src/styles/` 下创建主题变量文件：

**文件路径：** `src/styles/theme-{主题名称}.scss`

```scss
/**
 * 主题: {主题名称}
 * 主色调: {主色调}
 * 生成时间: {当前日期}
 */

// ===== 主题变量定义 =====
.theme-{主题名称} {
  // 菜单颜色
  --menuBg: {主色调深色变体};
  --menuText: rgba(255, 255, 255, 0.85);
  --menuActiveText: #ffffff;
  --menuHover: {主色调更深变体};
  --subMenuBg: {主色调最深变体};

  // Element Plus 主题色覆盖
  --el-color-primary: {主色调};
  --el-color-primary-light-3: {主色调浅色30%};
  --el-color-primary-light-5: {主色调浅色50%};
  --el-color-primary-light-7: {主色调浅色70%};
  --el-color-primary-light-9: {主色调浅色90%};
  --el-color-primary-dark-2: {主色调深色20%};
}
```

如果支持深色模式，追加暗黑变体：

```scss
// ===== 暗黑模式变体 =====
html.dark .theme-{主题名称} {
  --menuBg: rgba(0, 0, 0, 0.85);
  --menuText: rgba(255, 255, 255, 0.65);
  --menuActiveText: {主色调};
  --menuHover: rgba(255, 255, 255, 0.08);
  --subMenuBg: rgba(0, 0, 0, 0.9);

  --el-color-primary: {主色调};
  --el-color-primary-light-3: {主色调暗黑浅色30%};
  --el-color-primary-light-5: {主色调暗黑浅色50%};
  --el-color-primary-light-7: {主色调暗黑浅色70%};
  --el-color-primary-light-9: {主色调暗黑浅色90%};
  --el-color-primary-dark-2: {主色调暗黑深色20%};
}
```

### 步骤 2：配置 Element Plus 主题覆盖

在 `src/styles/index.scss` 中导入新主题文件：

```scss
// 在现有主题导入之后添加
@import "./theme-{主题名称}.scss";
```

### 步骤 3：配置主题切换逻辑

在 Settings Store 的主题类型定义中注册新主题。

**文件路径：** `src/cdp-admin/store/modules/settings.ts`

在主题选项中添加新主题标识：

```typescript
// 主题类型扩展 — 在类型定义中添加新主题
type ThemeType = "light" | "dark" | "blue" | "{主题名称}";
```

切换使用方式（组件中调用）：

```typescript
import { useSettingsStore } from "@/cdp-admin/store/modules/settings";

const settingsStore = useSettingsStore();

// 切换到新主题
settingsStore.changeSetting({ key: "theme", value: "{主题名称}" });
```

如需在设置面板中展示新主题选项：

```vue
<el-radio-group :model-value="currentTheme" @change="switchTheme">
  <el-radio label="light">浅色</el-radio>
  <el-radio label="dark">暗黑</el-radio>
  <el-radio label="blue">蓝色</el-radio>
  <el-radio label="{主题名称}">{主题中文名}</el-radio>
</el-radio-group>
```

## 完成后提醒

1. 确认 `src/styles/index.scss` 中已正确导入新主题文件，且导入顺序在 `theme.scss` 之后
2. 使用浏览器开发者工具检查 CSS 变量是否正确应用（检查 `document.documentElement` 上的类名）
3. 如果支持暗黑模式，需同时测试 `html.dark` 类名下的变量覆盖效果
4. 主色调的浅色/深色变体建议使用 HSL 色彩空间计算，保持色相一致
5. 主题切换后通过 `useStorage` 自动持久化到 localStorage，刷新页面后保持主题选择
