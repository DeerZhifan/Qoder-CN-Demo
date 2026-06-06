# 如何使用构建工具

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: Vite, 构建, 打包, pnpm, ESLint, Prettier, Stylelint, 插件, 代理, build, dev

---

## 概述

CDP Web 使用 Vite 作为构建工具，pnpm 作为包管理器（强制），集成 ESLint + Prettier + Stylelint + Commitlint 代码质量工具链。构建配置位于 `vite.config.ts`。

## 常用命令

```bash
# 启动开发服务器（端口 9527）
pnpm run dev

# 生产构建（含 vue-tsc 类型检查）
pnpm run build:prod

# JS/TS/Vue 代码检查并自动修复
pnpm run lint:eslint

# 代码格式化
pnpm run lint:prettier

# 样式检查并自动修复
pnpm run lint:stylelint

# 交互式 Git 提交（遵守 commitlint 规范）
pnpm run commit
```

## 开发服务器配置

### 修改端口

在 `.env.development` 中：

```bash
VITE_APP_PORT = 9527
```

### 配置后端代理

在 `vite.config.ts` 的 `server.proxy` 中：

```typescript
server: {
  host: "0.0.0.0",      // 允许 IP 访问
  port: Number(env.VITE_APP_PORT),
  open: true,            // 自动打开浏览器
  proxy: {
    [env.VITE_APP_BASE_API]: {
      changeOrigin: true,
      target: "http://172.17.1.28:28080",  // 修改此处切换后端
      rewrite: (path) =>
        path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
    },
  },
},
```

### 启用 Mock 数据

取消 `vite.config.ts` 中 `mockDevServerPlugin()` 的注释，同时将 `target` 切换到本地：

```typescript
plugins: [
  mockDevServerPlugin(),  // 取消注释
  // ...
],
```

Mock 接口定义在项目根目录的 `mock/` 目录下。新增 mock 接口后需重启开发服务器。

## 生产构建

```bash
pnpm run build:prod
```

构建产物输出到 `dist/` 目录，包含：
- `js/` — 打包后的 JS 文件
- `css/` — 打包后的样式文件
- `img/` — 图片资源
- `fonts/` — 字体资源
- `index.html` — 入口 HTML

构建优化配置：
- `chunkSizeWarningLimit: 2000` — 提高分包告警阈值
- `minify: "terser"` — 使用 Terser 压缩
- `drop_console: true` — 生产环境移除 console
- `drop_debugger: true` — 生产环境移除 debugger

## 添加 Vite 插件

在 `vite.config.ts` 的 `plugins` 数组中添加：

```typescript
import myPlugin from "my-vite-plugin";

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    vueJsx(),
    UnoCSS(),
    AutoImport({ ... }),
    Components({ ... }),
    myPlugin(),  // 添加新插件
  ],
}));
```

## 自动导入配置

### Vue API 自动导入

`unplugin-auto-import` 自动导入 `ref`、`reactive`、`computed`、`watch` 等 Vue API 以及 `@vueuse/core` 的组合式函数，无需手动 import。

### Element Plus 组件自动导入

`unplugin-vue-components` 自动按需注册 Element Plus 组件（如 `<el-button>`、`<el-table>`），无需手动 import。

### SVG 图标自动注册

`vite-plugin-svg-icons` 自动注册 `src/assets/icons/` 目录下的 SVG 文件，通过 `<SvgIcon name="icon-name" />` 使用。

## 代码质量工具

### ESLint 配置

配置文件：`.eslintrc.js` / `.eslintrc-auto-import.json`

```bash
# 检查并自动修复
pnpm run lint:eslint
```

### Prettier 配置

配置文件：`.prettierrc.js`

```bash
# 格式化代码
pnpm run lint:prettier
```

### Stylelint 配置

配置文件：`.stylelintrc.js`

```bash
# 检查并修复样式
pnpm run lint:stylelint
```

### Commitlint + cz-git

通过 Husky 在 `git commit` 时自动检查提交信息格式：

```bash
# 交互式提交（推荐）
pnpm run commit

# 提交信息格式
feat: 新功能
fix: Bug 修复
docs: 文档更新
style: 样式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具配置
```

## 路径别名

`@/` 映射到 `src/`，在 `vite.config.ts` 和 `tsconfig.json` 中配置：

```typescript
// vite.config.ts
resolve: {
  alias: {
    "@": resolve(__dirname, "src"),
  },
},
```

## 注意事项

> 注意：包管理器强制使用 pnpm，项目通过 `preinstall` 钩子限制。使用 npm 或 yarn 会报错。

> 注意：`auto-import` 和 `components` 的 `dts` 配置设为 `false`，类型声明文件需手动维护。如果 IDE 报类型错误，检查 `src/typings/` 下的声明文件。

> 注意：修改 `vite.config.ts`、`.env.*` 文件、或新增文件后需要重启开发服务器（`pnpm run dev`）。

> 注意：`import.meta.glob` 是 Vite 特性，仅在启动时扫描文件系统。新增 Vue 文件后需重启才能被动态导入发现。

> 注意：生产构建会自动移除 `console.log` 和 `debugger` 语句，开发时可放心使用。
