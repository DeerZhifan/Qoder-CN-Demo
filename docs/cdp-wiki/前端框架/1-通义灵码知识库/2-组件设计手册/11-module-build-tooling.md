# 构建与工具链手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: Vite, 构建, ESLint, Prettier, Stylelint, Husky, commitlint, pnpm, 插件

---

## 一、Vite 插件配置

源码位置：`vite.config.ts`

| 插件 | 功能 |
|------|------|
| `@vitejs/plugin-vue` | Vue 3 SFC 支持 |
| `@vitejs/plugin-vue-jsx` | JSX/TSX 语法支持 |
| `UnoCSS` | 原子化 CSS 引擎 |
| `unplugin-auto-import` | Vue/VueRouter/Pinia/@vueuse API 自动导入 |
| `unplugin-vue-components` | Element Plus + 自定义组件自动注册 |
| `unplugin-icons` | 图标组件自动生成（Element Plus 图标集） |
| `vite-plugin-svg-icons` | 本地 SVG 图标精灵图 |
| `vite-plugin-mock-dev-server` | Mock API 服务（默认关闭） |

### auto-import 配置

```typescript
AutoImport({
  imports: ["vue", "vue-router", "pinia", "@vueuse/core"],
  resolvers: [ElementPlusResolver(), IconsResolver()],
  eslintrc: { enabled: false },  // ESLint 兼容（当前关闭）
  dts: false,                     // 类型声明文件（当前关闭，使用预生成的静态文件）
}),
Components({
  resolvers: [
    ElementPlusResolver(),
    IconsResolver({ enabledCollections: ["ep"] }),
  ],
  dirs: ["src/**/components"],  // 自动扫描目录
  dts: false,
}),
```

### SVG 图标配置

```typescript
createSvgIconsPlugin({
  iconDirs: [path.resolve(process.cwd(), "src/assets/icons")],
  symbolId: "icon-[dir]-[name]",  // 图标 ID 格式
}),
```

---

## 二、构建优化

### 依赖预编译

```typescript
optimizeDeps: {
  include: [
    "vue", "vue-router", "pinia", "axios",
    "element-plus/es/components/form/style/css",
    "element-plus/es/components/table/style/css",
    // ... 50+ Element Plus 样式模块
  ],
}
```

预编译 Element Plus 按需导入的样式模块，避免开发环境冷启动缓慢。

### 生产构建

```typescript
build: {
  chunkSizeWarningLimit: 2000,  // 告警阈值 2000KB
  minify: "terser",
  terserOptions: {
    compress: {
      drop_console: true,     // 移除 console
      drop_debugger: true,    // 移除 debugger
    },
  },
  rollupOptions: {
    output: {
      chunkFileNames: "js/[name].[hash].js",
      entryFileNames: "js/[name].[hash].js",
      assetFileNames: (assetInfo) => {
        // 按类型分类: img/ | fonts/ | media/ | css/
      },
    },
  },
}
```

### CSS 预处理

```typescript
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

全局注入 SCSS 变量和 mixin，所有组件自动可用。

---

## 三、代码质量工具链

### ESLint

配置文件：`.eslintrc.cjs`

| 配置项 | 值 |
|--------|-----|
| 解析器 | `vue-eslint-parser` + `@typescript-eslint/parser` |
| 扩展 | `vue3-recommended` + `@typescript-eslint/recommended` + `prettier` |
| 特殊处理 | auto-import 的 globals（`.eslintrc-auto-import.json`） |
| 宽松规则 | 多个 TS 规则设为 warn 或 off（快速开发模式） |

### Prettier

配置文件：`.prettierrc.cjs`

```javascript
{
  printWidth: 80,           // 行宽
  tabWidth: 2,              // 缩进
  useTabs: false,           // 空格缩进
  semi: true,               // 分号
  singleQuote: false,       // 双引号
  trailingComma: "es5",     // 尾逗号
}
```

### Stylelint

配置文件：`.stylelintrc.cjs`

| 配置项 | 说明 |
|--------|------|
| 语法 | postcss-html（Vue）+ postcss-scss（SCSS） |
| 扩展 | standard + recommended-scss + recommended-vue + recess-order |
| 自定义 | 允许自定义属性和伪类 |

### Commitlint

配置文件：`.commitlint.config.cjs`

支持 11 种提交类型：

| 类型 | 说明 | 类型 | 说明 |
|------|------|------|------|
| `feat` | 新功能 | `fix` | Bug 修复 |
| `docs` | 文档 | `style` | 代码格式 |
| `refactor` | 重构 | `perf` | 性能优化 |
| `test` | 测试 | `build` | 构建 |
| `ci` | CI 配置 | `revert` | 回退 |
| `chore` | 杂项 | | |

提交格式：`type(scope): subject`

```bash
# 示例
feat(system): 新增用户批量导入功能
fix(login): 修复 CAS 登录回调地址错误
docs: 更新 API 接口文档
```

---

## 四、Git Hooks

### Husky + lint-staged

```json
// package.json
{
  "lint-staged": {
    "*.{ts,js}": ["eslint --fix"],
    "*.{json,cjs}": ["prettier --write"],
    "*.{vue,html}": ["eslint --fix", "prettier --write"],
    "*.{css,scss}": ["stylelint --fix"]
  }
}
```

| Hook | 触发时机 | 执行内容 |
|------|---------|---------|
| `pre-commit` | 提交前 | lint-staged（ESLint/Prettier/Stylelint 修复暂存文件） |
| `commit-msg` | 提交信息编写后 | commitlint 校验提交格式 |

---

## 五、常用命令速查

| 命令 | 说明 |
|------|------|
| `pnpm dev` | 启动开发服务器（端口 9527） |
| `pnpm build:prod` | 生产构建 + TypeScript 类型检查 |
| `pnpm lint:eslint` | ESLint 检查 + 自动修复 |
| `pnpm lint:prettier` | Prettier 格式化所有文件 |
| `pnpm lint:stylelint` | Stylelint 检查 + 自动修复 |
| `pnpm commit` | 交互式规范提交（cz-git） |

### 构建输出

```
dist/
├── index.html
├── js/
│   ├── main.[hash].js        # 入口
│   ├── [name].[hash].js      # 路由懒加载 chunk
│   └── vendor.[hash].js      # 第三方依赖
├── css/[name].[hash].css
├── img/
├── fonts/
└── media/
```

---

## 六、包管理器限制

```json
// package.json
{
  "scripts": {
    "preinstall": "npx only-allow pnpm"  // 仅允许 pnpm
  }
}
```

使用 `npm install` 或 `yarn install` 会被拦截，必须使用 `pnpm install`。

---

## 七、常见陷阱

1. **仅限 pnpm**: `preinstall` 钩子强制使用 pnpm，不能用 npm 或 yarn
2. **auto-import 类型**: 当前 `dts: false`，类型文件是预生成的静态版本，新增导入配置需手动更新
3. **optimizeDeps**: 新增 Element Plus 按需组件时，可能需要将其样式模块加入 `include` 列表
4. **Terser 移除 console**: 生产构建自动删除 `console` 和 `debugger`，测试环境注意
5. **构建包过大**: 检查是否误引入大型库，可用 `rollup-plugin-visualizer` 分析包组成
6. **Mock 默认关闭**: `vite-plugin-mock-dev-server` 在 `vite.config.ts` 中被注释，启用需取消注释
