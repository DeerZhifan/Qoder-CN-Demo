# 知识库管理后台前端技术方案

## 1. 项目概述

本文档为 CDP Wiki Browser 项目的知识库管理功能提供前端技术预研方案，涵盖组件选型、状态管理、路由设计、Mock 数据等核心内容。

**现有基础**:
- Vue 3.5.35 + Element Plus 2.14.1 + Vite 8.0.16
- 已实现 Wiki 浏览功能（左侧目录树 + 中间 Markdown 渲染 + 右侧 TOC）
- 当前使用 markdown-it 进行只读渲染

---

## 2. Markdown 编辑器选型

### 2.1 候选方案对比

| 维度 | **md-editor-v3** ⭐推荐 | mavon-editor | vditor |
|------|------------------------|--------------|--------|
| **Vue 3 兼容性** | ✅ 专为 Vue 3 设计，支持 `<script setup>` | ⚠️ 需使用 `@next` 版本，API 有变化 | ✅ 支持 Vue/React/Angular |
| **实时预览** | ✅ 双栏实时预览，同步滚动 | ✅ 双栏实时预览 | ✅ 支持 WYSIWYG/IR/SV 三种模式 |
| **样式定制能力** | ✅ 内置 6 种主题（default/github/vuepress/cyanosis/mk-cute/smart-blue），支持自定义 | ⚠️ 基础主题，定制较复杂 | ✅ 多主题支持 |
| **包体积 (gzip)** | 🟢 ~50KB（核心包），按需加载可更小 | 🔴 ~200KB | 🟡 ~120KB |
| **社区活跃度** | ✅ 活跃维护，GitHub stars ~500+ | ⚠️ 更新缓慢，维护不活跃 | ✅ 活跃，思源笔记背书，stars 5.8k+ |
| **TypeScript 支持** | ✅ 原生 TSX 开发，完整类型推断 | ❌ 无 TS 支持 | ✅ TypeScript 实现 |
| **扩展性** | ✅ 插件机制，支持 Mermaid/KaTeX/PDF 导出 | ⚠️ 功能固定，扩展有限 | ✅ 丰富插件生态 |
| **学习曲线** | 🟢 API 简洁，文档完善 | 🟡 配置项较多 | 🔴 API 复杂，上手成本高 |
| **与现有整合** | ✅ 可复用现有 markdown-it 解析逻辑 | ⚠️ 需替换现有渲染方案 | ⚠️ 需重构现有架构 |

### 2.2 最终推荐：**md-editor-v3**

**推荐理由**:

1. **轻量高效**: 核心包仅 50KB (gzip)，比 mavon-editor 小 75%，比 vditor 小 58%
2. **Vue 3 原生友好**: 专为 Vue 3 Composition API 设计，完美支持 `<script setup>` 语法
3. **按需加载**: v4.0.0+ 支持组件按需引入，进一步优化打包体积
4. **功能完备**: 
   - 内置工具栏（粗体/斜体/标题/代码块/表格/图片上传等）
   - 实时预览 + 同步滚动
   - 快捷键支持（Ctrl+S 保存等）
   - Prettier 格式化（CDN 方式，可选）
   - Mermaid 图表 + KaTeX 数学公式
   - 图片粘贴/拖拽上传
5. **主题灵活**: 6 种内置预览主题，与现有 github-markdown-css 风格一致
6. **易集成**: API 简洁，迁移成本低，可平滑替代现有 markdown-it 渲染

**安装命令**:
```bash
npm install md-editor-v3
```

**基本用法示例**:
```vue
<template>
  <MdEditor 
    v-model="content" 
    :toolbars="customToolbars"
    preview-theme="github"
    @save="handleSave"
  />
</template>

<script setup>
import { ref } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const content = ref('# 欢迎使用知识库')

const customToolbars = [
  'bold', 'italic', 'underline', 'strikeThrough',
  'title', 'quote', 'unorderedList', 'orderedList',
  'code', 'table', 'link', 'image',
  '|', 'undo', 'redo', 'fullscreen'
]

const handleSave = (value, html) => {
  console.log('Markdown:', value)
  console.log('HTML:', html)
}
</script>
```

---

## 3. Element Plus 组件使用要点

### 3.1 Tree 组件（目录树）

**适用场景**: 知识库分类目录展示

**核心配置**:
```vue
<el-tree
  :data="treeData"
  :props="{ label: 'name', children: 'children' }"
  node-key="id"
  highlight-current
  accordion
  @node-click="handleNodeClick"
>
  <template #default="{ node, data }">
    <span class="custom-tree-node">
      <el-icon><Document /></el-icon>
      <span>{{ node.label }}</span>
    </span>
  </template>
</el-tree>
```

**最佳实践**:
- 使用 `node-key` 唯一标识节点，便于后续操作
- `highlight-current` 高亮选中节点
- `accordion` 手风琴模式，同时只展开一个父节点
- 自定义节点插槽，添加图标和样式
- 大数据量时启用懒加载 (`lazy` + `load` 方法)

### 3.2 Table 组件（文档列表）

**适用场景**: 文档列表页，展示所有文档的元数据

**核心配置**:
```vue
<el-table
  :data="docList"
  stripe
  border
  style="width: 100%"
  @selection-change="handleSelectionChange"
>
  <el-table-column type="selection" width="55" />
  <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
  <el-table-column prop="category" label="分类" width="120" />
  <el-table-column prop="updateTime" label="更新时间" width="180" sortable />
  <el-table-column label="操作" width="200" fixed="right">
    <template #default="scope">
      <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
      <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
```

**高级特性**:
- **虚拟滚动**: 大数据量时设置 `height` 启用虚拟滚动
- **固定列**: `fixed="right"` 固定操作列
- **排序筛选**: `sortable` + `filters` 属性
- **自定义渲染**: 使用 `#default` 插槽自定义单元格内容
- **行样式**: `:row-class-name` 动态设置行样式

### 3.3 Dialog 组件（编辑弹窗/确认框）

**适用场景**: 新建/编辑文档、删除确认、表单提交

**核心配置**:
```vue
<el-dialog
  v-model="dialogVisible"
  title="编辑文档"
  width="80%"
  :close-on-click-modal="false"
  @close="handleDialogClose"
>
  <!-- 表单内容 -->
  <el-form :model="form" :rules="rules" ref="formRef">
    <el-form-item label="标题" prop="title">
      <el-input v-model="form.title" />
    </el-form-item>
    <!-- ... -->
  </el-form>
  
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit">确定</el-button>
  </template>
</el-dialog>
```

**最佳实践**:
- `:close-on-click-modal="false"` 防止误关闭
- 使用 `ref` 获取表单实例进行验证
- `@close` 事件重置表单状态
- 大表单时使用 `destroy-on-close` 销毁 DOM

### 3.4 TreeSelect 组件（分类选择器）

**适用场景**: 文档所属分类选择、层级结构选择

**核心配置**:
```vue
<el-tree-select
  v-model="selectedCategory"
  :data="categoryTree"
  :props="{ label: 'name', children: 'children' }"
  node-key="id"
  check-strictly
  show-checkbox
  placeholder="请选择分类"
  style="width: 240px"
/>
```

**最佳实践**:
- `check-strictly` 父子节点不关联
- `show-checkbox` 显示复选框（多选场景）
- 支持搜索过滤 (`filterable` + `filter-method`)
- 大数据量时启用懒加载

---

## 4. 路由结构设计

### 4.1 路由配置

**推荐路由结构**:

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/knowledge-base/list'
  },
  {
    path: '/knowledge-base',
    component: () => import('@/views/KnowledgeBaseLayout.vue'),
    children: [
      {
        path: 'list',
        name: 'DocList',
        component: () => import('@/views/DocList.vue'),
        meta: { title: '文档列表' }
      },
      {
        path: 'editor/:id?',
        name: 'DocEditor',
        component: () => import('@/views/DocEditor.vue'),
        meta: { title: '文档编辑' },
        props: true // 将路由参数作为 props 传递
      }
    ]
  },
  {
    path: '/wiki',
    name: 'WikiBrowser',
    component: () => import('@/views/WikiBrowser.vue'),
    meta: { title: 'Wiki 浏览' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### 4.2 路由说明

| 路由路径 | 组件 | 说明 |
|---------|------|------|
| `/` | 重定向 | 默认跳转到文档列表 |
| `/knowledge-base/list` | DocList.vue | 文档列表页，Table 展示所有文档 |
| `/knowledge-base/editor/new` | DocEditor.vue | 新建文档（id='new'） |
| `/knowledge-base/editor/:id` | DocEditor.vue | 编辑已有文档（id 为文档 ID） |
| `/wiki` | WikiBrowser.vue | 保留现有 Wiki 浏览功能 |

### 4.3 导航守卫示例

```javascript
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title || '知识库管理'
  next()
})
```

### 4.4 与现有 Wiki 浏览整合

**方案 A（推荐）**: 独立路由
- `/wiki` 保持现有浏览功能不变
- `/knowledge-base/*` 新增管理功能
- 顶部导航栏切换两个模块

**方案 B**: 嵌套路由
- 在现有 App.vue 中嵌入路由视图
- 通过侧边栏菜单切换浏览/管理模式

---

## 5. Mock 数据方案

### 5.1 方案选择：**vite-plugin-mock**

**优势**:
- 与 Vite 深度集成，配置简单
- 支持开发环境和生产环境
- 基于 mockjs，支持随机数据生成
- 自动拦截 axios/fetch 请求

### 5.2 实施步骤

#### Step 1: 安装依赖
```bash
npm install vite-plugin-mock mockjs -D
```

#### Step 2: 配置 vite.config.js
```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    viteMockServe({
      mockPath: 'mock',           // Mock 文件目录
      localEnabled: command === 'serve', // 仅开发环境启用
      logger: true,               // 控制台显示请求日志
      supportTs: false            // 使用 JS 文件
    })
  ],
  server: {
    port: 5173,
    proxy: {
      // 真实 API 代理（可选，用于混合模式）
      '/api/real': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/real/, '')
      }
    }
  }
}))
```

#### Step 3: 创建 Mock 文件

**目录结构**:
```
mock/
├── knowledge-base.js    # 知识库相关接口
└── _createProductionServer.js  # 生产环境 Mock（可选）
```

**mock/knowledge-base.js**:
```javascript
import Mock from 'mockjs'

const Random = Mock.Random

// 模拟文档列表
const docList = []
for (let i = 0; i < 20; i++) {
  docList.push({
    id: i + 1,
    title: Random.ctitle(5, 15),
    category: Random.pick(['前端框架', '后端框架', '运维指南']),
    content: Random.cparagraph(3, 10),
    createTime: Random.datetime('yyyy-MM-dd HH:mm:ss'),
    updateTime: Random.datetime('yyyy-MM-dd HH:mm:ss'),
    status: Random.pick(['published', 'draft'])
  })
}

// 模拟分类树
const categoryTree = [
  {
    id: 1,
    name: '前端框架',
    children: [
      { id: 11, name: 'Vue' },
      { id: 12, name: 'React' }
    ]
  },
  {
    id: 2,
    name: '后端框架',
    children: [
      { id: 21, name: 'Spring Boot' },
      { id: 22, name: 'Express' }
    ]
  }
]

export default [
  // 获取文档列表
  {
    url: '/api/knowledge-base/list',
    method: 'get',
    response: () => {
      return {
        code: 200,
        message: 'success',
        data: {
          list: docList,
          total: docList.length
        }
      }
    }
  },
  
  // 获取单个文档
  {
    url: '/api/knowledge-base/detail/:id',
    method: 'get',
    response: ({ query }) => {
      const doc = docList.find(d => d.id == query.id)
      return {
        code: 200,
        message: 'success',
        data: doc || null
      }
    }
  },
  
  // 新建文档
  {
    url: '/api/knowledge-base/create',
    method: 'post',
    response: ({ body }) => {
      const newDoc = {
        id: docList.length + 1,
        ...body,
        createTime: Random.now(),
        updateTime: Random.now()
      }
      docList.push(newDoc)
      return {
        code: 200,
        message: '创建成功',
        data: newDoc
      }
    }
  },
  
  // 更新文档
  {
    url: '/api/knowledge-base/update/:id',
    method: 'put',
    response: ({ body, query }) => {
      const index = docList.findIndex(d => d.id == query.id)
      if (index !== -1) {
        docList[index] = { ...docList[index], ...body, updateTime: Random.now() }
        return {
          code: 200,
          message: '更新成功',
          data: docList[index]
        }
      }
      return {
        code: 404,
        message: '文档不存在'
      }
    }
  },
  
  // 删除文档
  {
    url: '/api/knowledge-base/delete/:id',
    method: 'delete',
    response: ({ query }) => {
      const index = docList.findIndex(d => d.id == query.id)
      if (index !== -1) {
        docList.splice(index, 1)
        return {
          code: 200,
          message: '删除成功'
        }
      }
      return {
        code: 404,
        message: '文档不存在'
      }
    }
  },
  
  // 获取分类树
  {
    url: '/api/knowledge-base/categories',
    method: 'get',
    response: () => {
      return {
        code: 200,
        message: 'success',
        data: categoryTree
      }
    }
  }
]
```

### 5.3 Mock/真实 API 切换方案

**方案 A: 环境变量控制**

```javascript
// .env.development
VITE_API_BASE_URL=/api
VITE_USE_MOCK=true

// .env.production
VITE_API_BASE_URL=https://api.example.com
VITE_USE_MOCK=false
```

```javascript
// utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

export default request
```

```javascript
// vite.config.js
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command }) => ({
  plugins: [
    viteMockServe({
      localEnabled: command === 'serve' && import.meta.env.VITE_USE_MOCK === 'true'
    })
  ]
}))
```

**方案 B: 代理路径区分**

```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      // Mock 接口
      '/api/mock': {
        target: 'http://localhost:5173',
        changeOrigin: true
      },
      // 真实 API
      '/api/real': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/real/, '')
      }
    }
  }
})
```

**推荐**: 使用方案 A（环境变量），更灵活，便于 CI/CD 集成。

---

## 6. 状态管理方案

### 6.1 决策依据

| 场景 | 推荐方案 | 理由 |
|------|---------|------|
| 组件内部状态 | `ref` / `reactive` | 轻量，无需额外依赖 |
| 父子组件通信 | `props` + `emit` | Vue 标准做法 |
| 跨组件共享（≤3 个组件） | 导出 `reactive` 对象 | 简单场景，避免过度设计 |
| 全局状态（用户信息、主题等） | **Pinia** | 官方推荐，TypeScript 友好 |
| 复杂业务逻辑 | Pinia Store | 集中管理，易于调试和维护 |

### 6.2 本项目推荐：**暂不需要 Pinia**

**理由**:
1. **项目规模小**: 当前仅新增 2 个页面（列表 + 编辑），状态复杂度低
2. **现有架构简单**: Wiki 浏览功能仅用 `ref`/`reactive` 即可满足
3. **避免过度设计**: 引入 Pinia 会增加学习成本和包体积（~1KB gzip）
4. **未来可扩展**: 若后续增加用户认证、权限管理等全局状态，再引入 Pinia

### 6.3 推荐状态管理策略

**局部状态** (各组件内部):
```vue
<script setup>
import { ref, reactive } from 'vue'

// 基本类型用 ref
const searchKeyword = ref('')
const dialogVisible = ref(false)

// 对象用 reactive
const form = reactive({
  title: '',
  content: '',
  categoryId: null
})
</script>
```

**跨组件共享** (简单场景):
```javascript
// stores/docStore.js
import { reactive } from 'vue'

export const docStore = reactive({
  currentDoc: null,
  selectedDocs: [],
  
  setCurrentDoc(doc) {
    this.currentDoc = doc
  },
  
  clearCurrentDoc() {
    this.currentDoc = null
  }
})
```

```vue
<!-- 组件中使用 -->
<script setup>
import { docStore } from '@/stores/docStore'
</script>
```

**何时引入 Pinia**:
- 需要持久化状态（localStorage/sessionStorage）
- 需要 DevTools 调试支持
- 需要时间旅行（撤销/重做）
- 多个不相关组件频繁读写同一状态
- 团队规模扩大，需要统一状态管理规范

---

## 7. 新增 npm 依赖清单

### 7.1 必需依赖

```json
{
  "dependencies": {
    "vue-router": "^4.5.0",        // 路由管理
    "axios": "^1.7.0",             // HTTP 客户端
    "md-editor-v3": "^4.20.0"      // Markdown 编辑器
  },
  "devDependencies": {
    "vite-plugin-mock": "^3.0.0",  // Mock 数据插件
    "mockjs": "^1.1.0"             // Mock 数据生成
  }
}
```

### 7.2 可选依赖（根据需求）

```json
{
  "dependencies": {
    "pinia": "^2.2.0",             // 状态管理（如需）
    "@element-plus/icons-vue": "^2.3.1"  // 已安装，确保引入
  }
}
```

### 7.3 安装命令

```bash
# 核心依赖
npm install vue-router axios md-editor-v3

# 开发依赖
npm install vite-plugin-mock mockjs -D

# 可选：状态管理
npm install pinia
```

---

## 8. 与现有 Wiki 浏览功能整合

### 8.1 整合策略

**保持现有功能不变**:
- `docs/cdp-wiki/` 目录下的 Markdown 文件继续用于只读浏览
- 现有 `WikiSidebar`、`WikiContent`、`WikiToc` 组件保持不变
- 新增 `/wiki` 路由入口，保留原有体验

**新增管理功能**:
- 数据库/文件系统存储可编辑文档
- `/knowledge-base/list` 和 `/knowledge-base/editor` 提供 CRUD 功能
- 顶部导航栏增加"浏览模式"和"管理模式"切换

### 8.2 顶部导航改造示例

```vue
<!-- App.vue 或 Layout 组件 -->
<header class="app-header">
  <div class="header-left">
    <h1>CDP 知识库</h1>
    <nav class="nav-menu">
      <router-link to="/wiki">Wiki 浏览</router-link>
      <router-link to="/knowledge-base/list">文档管理</router-link>
    </nav>
  </div>
</header>

<router-view />
```

### 8.3 数据流设计

```
Wiki 浏览模式:
docs/cdp-wiki/*.md → buildTree() → WikiSidebar → WikiContent (markdown-it 渲染)

文档管理模式:
API (Mock/Real) → axios → DocList/DocEditor → md-editor-v3 → API 保存
```

---

## 9. 实施建议

### 9.1 开发顺序

1. **Phase 1**: 路由 + 基础布局
   - 安装 vue-router
   - 创建路由配置文件
   - 搭建 Layout 组件（顶部导航 + 侧边栏 + 主内容区）

2. **Phase 2**: Mock 数据 + API 封装
   - 配置 vite-plugin-mock
   - 创建 Mock 数据文件
   - 封装 axios 请求工具

3. **Phase 3**: 文档列表页
   - 实现 DocList.vue
   - 集成 ElTable + ElPagination
   - 对接列表 API

4. **Phase 4**: 文档编辑器
   - 安装 md-editor-v3
   - 实现 DocEditor.vue
   - 集成新建/编辑/保存功能

5. **Phase 5**: 整合与优化
   - 顶部导航切换
   - 权限控制（如需）
   - 性能优化（懒加载、代码分割）

### 9.2 注意事项

- **样式隔离**: 使用 `<style scoped>` 避免样式冲突
- **错误处理**: 所有 API 调用需捕获异常并提示用户
- **加载状态**: 使用 `ElLoading` 或骨架屏提升体验
- **响应式设计**: 考虑移动端适配（Element Plus 自带响应式）
- **SEO 友好**: 使用 `vue-meta` 或手动设置 `<title>` 和 `<meta>`

---

## 10. 总结

### 核心技术选型

| 模块 | 方案 | 理由 |
|------|------|------|
| Markdown 编辑器 | **md-editor-v3** | 轻量、Vue 3 原生、功能完备、易集成 |
| 路由管理 | **vue-router 4** | Vue 官方路由，成熟稳定 |
| HTTP 客户端 | **axios** | 行业标准，拦截器强大 |
| Mock 数据 | **vite-plugin-mock** | Vite 深度集成，配置简单 |
| 状态管理 | **ref/reactive** | 项目规模小，避免过度设计 |
| UI 组件库 | **Element Plus** | 已安装，组件丰富 |

### 预期收益

- ✅ 快速实现知识库 CRUD 功能
- ✅ 保持与现有 Wiki 浏览功能兼容
- ✅ 轻量级方案，包体积可控
- ✅ 易于维护和扩展

### 下一步行动

1. 执行依赖安装命令
2. 创建路由配置文件
3. 搭建 Mock 数据环境
4. 开发文档列表页原型
5. 集成 md-editor-v3 编辑器

---

**文档版本**: v1.0  
**最后更新**: 2026-06-07  
**作者**: 前端架构专家组
