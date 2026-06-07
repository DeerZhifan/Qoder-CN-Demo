# 知识库浏览界面开发计划

## 技术选型

- **构建工具**: Vite 6
- **框架**: Vue 3 (Composition API + `<script setup>`)
- **UI 组件库**: Element Plus
- **Markdown 渲染**: markdown-it + highlight.js
- **Markdown 样式**: github-markdown-css
- **目录解析**: 基于 markdown-it 提取 H1-H3 标题生成 TOC

---

## 任务 1：项目初始化与依赖安装

**目标**: 在根目录初始化 Vite + Vue3 项目并安装必要依赖。

**文件变更**:
- 新建 `package.json`
- 新建 `vite.config.js`
- 新建 `index.html`
- 新建 `src/main.js`、`src/App.vue` 等基础文件

**命令**:
```bash
npm create vite@latest . -- --template vue
npm install
npm install element-plus markdown-it highlight.js github-markdown-css
npm install -D @types/markdown-it
```

---

## 任务 2：目录扫描与数据模型构建

**目标**: 使用 `import.meta.glob` 扫描 `../docs/cdp-wiki/**/*.md` 文件，构建树形数据结构。

**核心逻辑**:
- 利用 `import.meta.glob('../docs/cdp-wiki/**/*.md', { eager: true, as: 'raw' })` 批量加载 Markdown 原始文本
- 解析文件路径，按 `/` 分割构建目录树（TreeNode 结构）
- 每个节点包含：`name`(显示名), `path`(文件路径), `isFile`, `children`, `content`
- 目录节点支持展开/折叠，文件节点可点击切换

**文件**: `src/utils/buildTree.js`

---

## 任务 3：左侧目录树组件（WikiSidebar）

**目标**: 实现可展开折叠、支持点击切换的目录树。

**实现要点**:
- 使用 Element Plus 的 `el-tree` 组件展示树形数据
- 自定义 TreeNode，支持图标区分文件夹/文件
- 点击文件节点时，emit `select` 事件传递文件内容
- 默认展开第一层，支持搜索过滤（可选增强）

**文件**: `src/components/WikiSidebar.vue`

---

## 任务 4：中间 Markdown 渲染组件（WikiContent）

**目标**: 渲染选中的 Markdown 内容，支持代码高亮与表格。

**实现要点**:
- 使用 `markdown-it` 将 Markdown 转为 HTML
- 集成 `highlight.js` 实现代码高亮（自动识别语言）
- 引入 `github-markdown-css` 提供标准 Markdown 样式（表格、代码块等）
- 渲染后的内容需为 H1-H3 标题生成唯一锚点 ID，供 TOC 跳转使用
- 监听滚动事件，实时计算当前可见章节

**文件**: `src/components/WikiContent.vue`

---

## 任务 5：右侧 TOC 大纲组件（WikiToc）

**目标**: 提取当前文档 H1-H3 标题生成 TOC，支持点击锚点跳转和滚动高亮。

**实现要点**:
- 解析当前 Markdown 内容，提取所有 `#`、`##`、`###` 标题
- 为每个标题生成锚点链接（如 `#section-1-2`）
- 点击 TOC 项时平滑滚动到对应章节
- 使用 `IntersectionObserver` 监听正文区域，高亮当前可见章节对应的 TOC 项
- 当前高亮项自动滚动到可视区域

**文件**: `src/components/WikiToc.vue`

---

## 任务 6：主布局与状态管理

**目标**: 组装三栏布局，管理当前选中文档状态。

**实现要点**:
- 使用 Element Plus 的 `el-container`、`el-aside`、`el-main` 实现三栏弹性布局
- 左栏固定宽度（约 280px），右栏固定宽度（约 220px），中间自适应
- 使用 Vue `ref` 管理当前选中的文档内容
- 首次加载默认选中第一个 Markdown 文件并渲染
- 响应式处理：小屏幕下隐藏侧边栏（可选，以桌面端为主）

**文件**: `src/App.vue`

---

## 任务 7：样式与体验优化

**目标**: 统一视觉风格，提升使用体验。

**实现要点**:
- 引入 Element Plus 主题样式
- 自定义滚动条样式（WebKit）
- 代码块添加 copy 按钮（可选）
- 目录树当前选中项高亮
- TOC 当前章节高亮样式（如左侧边框、加粗）

**文件**: `src/style.css`

---

## 文件结构预期

```
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue
    ├── components/
    │   ├── WikiSidebar.vue   # 左侧目录树
    │   ├── WikiContent.vue   # 中间 Markdown 渲染
    │   └── WikiToc.vue       # 右侧 TOC 大纲
    ├── utils/
    │   ├── buildTree.js      # 目录树构建
    │   └── tocParser.js      # TOC 解析
    └── style.css
```

---

## 验证方式

1. 执行 `npm run dev` 启动开发服务器
2. 打开浏览器访问 `http://localhost:5173`
3. 验证：左侧目录树与 `docs/cdp-wiki` 结构一致，点击文件后中间渲染 Markdown，右侧 TOC 与正文章节联动
