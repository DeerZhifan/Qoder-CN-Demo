<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import WikiSidebar from './components/WikiSidebar.vue'
import WikiContent from './components/WikiContent.vue'
import WikiToc from './components/WikiToc.vue'
import { buildTree, getFirstDocPath, loadDocContent, findFilePathByPath } from './utils/buildTree.js'

// 目录树数据
const treeData = ref([])

// 当前选中的文档路径
const selectedPath = ref('')

// 当前文档内容
const currentContent = ref('')

// 是否加载中
const isLoading = ref(false)

// 目录是否为空
const isEmpty = computed(() => treeData.value.length === 0)

// 当前活跃的 heading ID（用于 scrollspy）
const activeHeadingId = ref('')

// WikiContent 组件引用
const contentRef = ref(null)

// 初始化
onMounted(async () => {
  // 构建目录树
  treeData.value = buildTree()
  
  if (isEmpty.value) return
  
  // 默认选中第一个文档
  const firstPath = getFirstDocPath(treeData.value)
  if (firstPath) {
    await handleDocSelect(firstPath)
  }
})

// 选择文档（异步懒加载）
const handleDocSelect = async (path) => {
  selectedPath.value = path
  activeHeadingId.value = ''
  isLoading.value = true
  currentContent.value = ''
  
  try {
    const filePath = findFilePathByPath(treeData.value, path)
    const content = filePath ? await loadDocContent(filePath) : ''
    currentContent.value = content || ''
  } catch (e) {
    console.error('文档加载失败:', e)
    currentContent.value = ''
  } finally {
    isLoading.value = false
  }
  
  // 滚动到顶部
  nextTick(() => {
    const contentBody = document.querySelector('.content-body')
    if (contentBody) {
      contentBody.scrollTop = 0
    }
  })
}

// 处理正文滚动事件（scrollspy）
const handleContentScroll = (headingId) => {
  activeHeadingId.value = headingId
}

// 处理 TOC 锚点点击
const handleAnchorClick = (anchorId) => {
  if (contentRef.value) {
    contentRef.value.scrollToAnchor(anchorId)
  }
}
</script>

<template>
  <div class="wiki-app">
    <!-- 顶部导航栏 -->
    <header class="wiki-header">
      <div class="header-left">
        <h1 class="app-title">CDP 知识库</h1>
      </div>
      <div class="header-right">
        <span class="current-doc" v-if="selectedPath">
          {{ selectedPath.split('/').pop().replace('.md', '') }}
        </span>
      </div>
    </header>
    
    <!-- 主内容区域 -->
    <div class="wiki-main">
      <!-- 左侧目录树 -->
      <aside class="wiki-sidebar">
        <WikiSidebar
          :tree-data="treeData"
          :selected-path="selectedPath"
          @select="handleDocSelect"
        />
      </aside>
      
      <!-- 中间 Markdown 渲染 -->
      <main class="wiki-content">
        <div v-if="isEmpty" class="empty-state">
          <el-empty description="知识库暂无文档，请在 docs/cdp-wiki/ 目录下添加 .md 文件" />
        </div>
        <div v-else-if="isLoading" class="loading-state">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <span>文档加载中...</span>
        </div>
        <WikiContent
          v-else
          ref="contentRef"
          :content="currentContent"
          @scroll="handleContentScroll"
        />
      </main>
      
      <!-- 右侧 TOC 大纲 -->
      <aside class="wiki-toc-panel">
        <WikiToc
          :content="currentContent"
          :active-heading-id="activeHeadingId"
          @anchor-click="handleAnchorClick"
        />
      </aside>
    </div>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  overflow: hidden;
}

#app {
  height: 100%;
}
</style>

<style scoped>
.wiki-app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

.wiki-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.app-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.current-doc {
  font-size: 13px;
  color: #909399;
  padding: 4px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.wiki-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.wiki-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  overflow: hidden;
}

.wiki-content {
  flex: 1;
  overflow: hidden;
  background: #fff;
}

.wiki-toc-panel {
  width: 220px;
  flex-shrink: 0;
  border-left: 1px solid #e4e7ed;
  background: #fff;
  overflow: hidden;
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #909399;
  font-size: 15px;
}
</style>
