<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { parseToc } from '@/utils/tocParser.js'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  activeHeadingId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['anchor-click'])

// TOC 列表容器引用（用于自动滚动）
const tocListRef = ref(null)

// 解析后的 TOC 数据
const tocItems = computed(() => {
  return parseToc(props.content)
})

// 当前活跃的 TOC 项
const currentActiveId = computed(() => props.activeHeadingId)

// 点击 TOC 项
const handleTocClick = (item) => {
  emit('anchor-click', item.id)
}

// 自动滚动 TOC 列表，使当前活跃项保持在可视区域
const scrollToActiveItem = () => {
  if (!tocListRef.value || !currentActiveId.value) return
  
  const activeElement = tocListRef.value.querySelector(`[data-id="${currentActiveId.value}"]`)
  if (activeElement) {
    activeElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 监听活跃 ID 变化，自动滚动 TOC
watch(currentActiveId, () => {
  nextTick(() => {
    scrollToActiveItem()
  })
})

// 获取 TOC 项的层级样式
const getTocStyle = (level) => {
  return {
    paddingLeft: `${(level - 1) * 16}px`
  }
}
</script>

<template>
  <div class="wiki-toc">
    <div class="toc-header">
      <span class="toc-title">目录</span>
    </div>
    <div class="toc-list" ref="tocListRef">
      <template v-if="tocItems.length > 0">
        <div
          v-for="item in tocItems"
          :key="item.id"
          :data-id="item.id"
          class="toc-item"
          :class="{
            'toc-item-active': currentActiveId === item.id,
            [`toc-level-${item.level}`]: true
          }"
          :style="getTocStyle(item.level)"
          @click="handleTocClick(item)"
        >
          <span class="toc-text">{{ item.text }}</span>
        </div>
      </template>
      <div v-else class="toc-empty">
        <span>暂无目录</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wiki-toc {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.toc-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.toc-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.toc-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.toc-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.toc-item:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.toc-item-active {
  color: #409eff;
  background-color: #ecf5ff;
  border-left-color: #409eff;
  font-weight: 500;
}

.toc-level-1 {
  font-weight: 500;
}

.toc-level-2 {
  font-size: 13px;
}

.toc-level-3 {
  font-size: 12px;
  color: #909399;
}

.toc-level-3.toc-item-active {
  color: #409eff;
}

.toc-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.toc-empty {
  padding: 40px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
