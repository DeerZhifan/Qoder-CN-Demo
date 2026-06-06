---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/11-module-build-tooling.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-build.md
---

# CDP 构建配置规则

## 适用场景

- 修改或新建 `vite.config.ts` 构建配置
- 配置开发服务器代理（后端 API 转发）
- 调整生产构建优化策略（分包、压缩、资源分类）
- 添加或调整 Vite 插件
- 修改环境变量与端口配置
- 执行构建、开发、代码检查等命令

## 前置依赖

- **包管理器**: 仅限 pnpm（`preinstall` 钩子强制，禁止 npm/yarn）
- **构建工具**: Vite 5+，配置入口 `vite.config.ts`
- **核心插件**: `@vitejs/plugin-vue`, `@vitejs/plugin-vue-jsx`, `unocss/vite`
- **自动导入**: `unplugin-auto-import`, `unplugin-vue-components`
- **图标**: `unplugin-icons`, `vite-plugin-svg-icons`
- **压缩**: `terser`（生产构建必需）

## 配置要点

### 1. Vite 配置结构

`vite.config.ts` 使用函数式导出，通过 `loadEnv` 读取环境变量：

```typescript
export default defineConfig(({ mode }: ConfigEnv): UserConfig => {
  const env = loadEnv(mode, process.cwd());
  return {
    resolve: { alias: { "@": resolve(__dirname, "src") } },
    css: { /* SCSS 预处理 */ },
    server: { /* 代理配置 */ },
    plugins: [ /* 插件列表 */ ],
    optimizeDeps: { /* 预编译 */ },
    build: { /* 生产构建 */ },
  };
});
```

### 2. 代理配置

代理前缀从 `.env.development` 的 `VITE_APP_BASE_API` 读取，target 指向后端服务地址：

```typescript
proxy: {
  [env.VITE_APP_BASE_API]: {
    changeOrigin: true,
    target: "http://172.17.1.28:28080",
    rewrite: (path) =>
      path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
    xfwd: true,
  },
},
```

### 3. 生产构建优化

```typescript
build: {
  target: "esnext",
  chunkSizeWarningLimit: 2000,
  minify: "terser",
  terserOptions: {
    compress: {
      keep_infinity: true,
      drop_console: true,
      drop_debugger: true,
    },
    format: { comments: false },
  },
  rollupOptions: {
    output: {
      entryFileNames: "js/[name].[hash].js",
      chunkFileNames: "js/[name].[hash].js",
      assetFileNames: (assetInfo) => {
        // 按类型分类: img/ | fonts/ | media/ | css/
      },
    },
  },
},
```

### 4. SCSS 全局注入

```typescript
css: {
  preprocessorOptions: {
    scss: {
      javascriptEnabled: true,
      additionalData: `
        @use "@/styles/variables.scss" as *;
        @use "@/styles/mixin.scss" as *;
      `,
    },
  },
},
```

### 5. 依赖预编译

新增 Element Plus 按需组件时，需将其样式模块加入 `optimizeDeps.include`：

```typescript
optimizeDeps: {
  include: [
    "vue", "vue-router", "pinia", "axios",
    "element-plus/es/components/form/style/css",
    "element-plus/es/components/table/style/css",
    // 新增组件的样式路径...
  ],
},
```

### 6. 常用命令

| 命令 | 说明 |
|------|------|
| `pnpm run dev` | 启动开发服务器（端口 9527） |
| `pnpm run build:prod` | 生产构建 + vue-tsc 类型检查 |
| `pnpm run lint:eslint` | ESLint 检查 + 自动修复 |
| `pnpm run lint:prettier` | Prettier 格式化 |
| `pnpm run lint:stylelint` | Stylelint 检查 + 自动修复 |
| `pnpm run commit` | 交互式规范提交（cz-git） |

## 代码模式

### 推荐写法

- 使用 `defineConfig` 函数式导出，通过 `loadEnv` 获取环境变量
- 代理 target 地址集中管理，通过注释标注各环境用途
- `optimizeDeps.include` 列出所有 Element Plus 按需导入的样式模块
- `auto-import` 的 `dts: false`，使用预生成的静态类型声明文件
- 静态资源按类型（img/fonts/media/css）分目录输出
- 路径别名统一使用 `@/` 映射到 `src/`

### 禁止事项

- **禁止** 使用 npm 或 yarn 安装依赖，必须使用 pnpm
- **禁止** 在生产构建中保留 `console.log` 和 `debugger`（terser 已配置自动移除）
- **禁止** 删除或修改 `preinstall` 钩子中的 `only-allow pnpm` 限制
- **禁止** 将 `auto-import` 的 `dts` 设为 `true` 后不更新 `.eslintrc-auto-import.json`
- **禁止** 修改 `@/` 路径别名的映射目标（必须指向 `src/`）
- **禁止** 在未重启开发服务器的情况下期望 `vite.config.ts` 或 `.env.*` 的修改生效
- **禁止** 移除 `optimizeDeps.include` 中已有的 Element Plus 样式模块（会导致冷启动变慢）
