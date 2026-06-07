<template>
  <div class="wiki-layout">
    <el-container>
      <el-aside width="280px" class="sidebar">
        <WikiSidebar
          :tree-data="treeData"
          :selected-path="selectedPath"
          @select="handleDocSelect"
        />
      </el-aside>
      <el-main>
        <router-view 
          :content="currentContent"
          :tree-data="treeData"
          :selected-path="selectedPath"
          @scroll="handleContentScroll"
        />
      </el-main>
      <el-aside width="220px" class="toc-panel">
        <WikiToc
          :content="currentContent"
          :active-heading-id="activeHeadingId"
          @anchor-click="handleAnchorClick"
        />
      </el-aside>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import WikiSidebar from '@/components/WikiSidebar.vue'
import WikiToc from '@/components/WikiToc.vue'
import { buildTree, getFirstDocPath, loadDocContent, findFilePathByPath } from '@/utils/buildTree.js'

// 目录树数据
const treeData = ref([])

// 当前选中的文档路径
const selectedPath = ref('')

// 当前文档内容
const currentContent = ref('')

// 是否加载中
const isLoading = ref(false)

// 当前活跃的 heading ID（用于 scrollspy）
const activeHeadingId = ref('')

// 初始化
onMounted(async () => {
  // 构建目录树
  treeData.value = buildTree()
  
  if (treeData.value.length === 0) return
  
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
}

// 处理正文滚动事件（scrollspy）
const handleContentScroll = (headingId) => {
  activeHeadingId.value = headingId
}

// 处理 TOC 锚点点击
const handleAnchorClick = (anchorId) => {
  // 这里可以通过 provide/inject 或直接调用子组件方法实现
  console.log('Anchor clicked:', anchorId)
}
</script>

<style scoped>
.wiki-layout {
  height: 100vh;
}
.sidebar {
  border-right: 1px solid #e4e7ed;
  background: #fff;
  overflow: hidden;
}
.toc-panel {
  border-left: 1px solid #e4e7ed;
  background: #fff;
  overflow: hidden;
}
</style>
