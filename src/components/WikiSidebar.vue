<script setup>
import { ref, computed, watch } from 'vue'
import { Folder, Document, Search } from '@element-plus/icons-vue'
import { filterTree, toElTreeData } from '@/utils/buildTree.js'
const props = defineProps({
  treeData: {
    type: Array,
    required: true
  },
  selectedPath: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])

// 搜索关键词
const searchKeyword = ref('')

// 树组件引用
const treeRef = ref(null)

// 过滤后的树形数据
const filteredTreeData = computed(() => {
  const elTreeData = toElTreeData(props.treeData)
  if (!searchKeyword.value) {
    return elTreeData
  }
  return filterTree(elTreeData, searchKeyword.value)
})

// 默认展开的节点（递归获取所有目录节点的 id，展开前两层）
const defaultExpandedKeys = computed(() => {
  const keys = []
  const collectKeys = (nodes, depth) => {
    nodes.forEach(node => {
      if (!node.isFile && depth < 2) {
        keys.push(node.id)
        if (node.children) {
          collectKeys(node.children, depth + 1)
        }
      }
    })
  }
  collectKeys(props.treeData, 0)
  return keys
})

// 自定义树节点过滤方法
const filterNode = (value, data) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

// 监听搜索关键词变化
watch(searchKeyword, (val) => {
  if (treeRef.value) {
    treeRef.value.filter(val)
  }
})

// 树节点点击事件
const handleNodeClick = (data) => {
  if (data.isFile) {
    emit('select', data.id)
  }
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
}
</script>

<template>
  <div class="wiki-sidebar">
    <!-- 搜索框 -->
    <div class="sidebar-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文档..."
        clearable
        @clear="clearSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <!-- 目录树 -->
    <div class="sidebar-tree">
      <el-tree
        ref="treeRef"
        :data="filteredTreeData"
        :props="{ children: 'children', label: 'label' }"
        :default-expanded-keys="defaultExpandedKeys"
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        node-key="id"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <el-icon :size="16" :class="data.isFile ? 'file-icon' : 'folder-icon'">
              <Document v-if="data.isFile" />
              <Folder v-else />
            </el-icon>
            <span class="node-label" :title="data.label">{{ data.label }}</span>
          </div>
        </template>
      </el-tree>
      
      <!-- 无搜索结果提示 -->
      <div v-if="searchKeyword && filteredTreeData.length === 0" class="no-result">
        <el-empty description="无匹配文档" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.wiki-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.sidebar-search {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}

.tree-node .file-icon {
  color: #909399;
  margin-right: 6px;
}

.tree-node .folder-icon {
  color: #E6A23C;
  margin-right: 6px;
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.no-result {
  padding: 40px 16px;
}

/* 自定义树样式 */
:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: 32px;
  padding-left: 8px !important;
}

:deep(.el-tree-node__content:hover) {
  background-color: #ecf5ff;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #ecf5ff;
  color: #409eff;
}

:deep(.el-tree-node.is-current > .el-tree-node__content .node-label) {
  font-weight: 500;
}
</style>
