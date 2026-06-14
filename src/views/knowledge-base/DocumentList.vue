<template>
  <div class="document-list">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
        <span style="font-weight: bold;">文档筛选</span>
        <el-button type="primary" size="small" @click="handleRefreshStats">刷新统计</el-button>
      </div>
      <el-form :inline="true">
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="请选择" clearable style="width: 200px">
            <el-option v-for="cat in flatCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="filters.title" placeholder="请输入标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区 -->
    <el-card class="table-card">
      <el-table :data="documents" v-loading="loading" border stripe>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="showPreview(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              v-if="row.status === 'DRAFT'" 
              size="small" 
              type="success" 
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button 
              v-if="row.status === 'PUBLISHED'" 
              size="small" 
              type="warning" 
              @click="handleOffline(row)"
            >
              下线
            </el-button>
            <el-button size="small" @click="showVersionHistory(row)">版本</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadDocuments"
          @size-change="loadDocuments"
        />
      </div>
    </el-card>

    <!-- Markdown预览Dialog -->
    <el-dialog v-model="previewVisible" title="文档预览" width="60%" destroy-on-close>
      <div class="markdown-body" v-html="renderedContent"></div>
    </el-dialog>

    <!-- 版本历史Dialog -->
    <el-dialog v-model="versionHistoryVisible" title="版本历史" width="70%" destroy-on-close>
      <el-table :data="versionList" v-loading="versionLoading" border>
        <el-table-column prop="version" label="版本号" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="createBy" label="创建人" width="120" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { pageDocuments, getCategoryTree, publishDocument, offlineDocument, deleteDocument, getDocument, getVersionList, rollbackVersion } from '@/api/document'
import { getCategoryDocumentCount } from '@/api/category'

const router = useRouter()
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})

// 状态
const loading = ref(false)
const documents = ref([])
const categories = ref([])
const flatCategories = ref([])
const filters = reactive({ categoryId: null, status: '', title: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const previewVisible = ref(false)
const renderedContent = ref('')
const versionHistoryVisible = ref(false)
const currentDocumentId = ref(null)
const versionLoading = ref(false)
const versionList = ref([])

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const params = {
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    // 过滤空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const result = await pageDocuments(params)
    documents.value = result.records || result.list || []
    pagination.total = result.total || 0
  } catch (error) {
    console.error('加载文档失败:', error)
    ElMessage.error('加载文档失败')
  } finally {
    loading.value = false
  }
}

// 加载分类树并展平
const loadCategories = async () => {
  try {
    const tree = await getCategoryTree()
    categories.value = tree
    flatCategories.value = flattenTree(tree)
  } catch (error) {
    console.error('加载分类失败:', error)
    ElMessage.error('加载分类失败')
  }
}

// 递归展平树形结构
const flattenTree = (tree, result = []) => {
  tree.forEach(node => {
    result.push({ id: node.id, name: node.name })
    if (node.children && node.children.length > 0) {
      flattenTree(node.children, result)
    }
  })
  return result
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadDocuments()
}

// 重置
const handleReset = () => {
  filters.categoryId = null
  filters.status = ''
  filters.title = ''
  pagination.pageNum = 1
  loadDocuments()
}

// 编辑
const handleEdit = (row) => {
  router.push(`/knowledge-base/editor/${row.id}`)
}

// 发布
const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确认发布该文档?', '提示', { 
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await publishDocument(row.id)
    ElMessage.success('发布成功')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发布失败:', error)
      ElMessage.error('发布失败')
    }
  }
}

// 下线
const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确认下线该文档?', '提示', { 
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await offlineDocument(row.id)
    ElMessage.success('下线成功')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下线失败:', error)
      ElMessage.error('下线失败')
    }
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该文档?此操作不可恢复!', '警告', { 
      type: 'error',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 预览
const showPreview = async (row) => {
  try {
    // 获取完整文档内容
    const doc = await getDocument(row.id)
    renderedContent.value = DOMPurify.sanitize(md.render(doc.content || '# 暂无内容'))
    previewVisible.value = true
  } catch (error) {
    console.error('加载文档内容失败:', error)
    ElMessage.error('加载文档内容失败')
  }
}

// 版本历史
const showVersionHistory = async (row) => {
  currentDocumentId.value = row.id
  versionHistoryVisible.value = true
  await loadVersionList()
}

// 加载版本列表
const loadVersionList = async () => {
  versionLoading.value = true
  try {
    versionList.value = await getVersionList(currentDocumentId.value)
  } catch (error) {
    console.error('加载版本列表失败:', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    versionLoading.value = false
  }
}

// 版本回滚
const handleRollback = async (row) => {
  try {
    await ElMessageBox.confirm(`确认回滚到版本 ${row.version}?`, '提示', { 
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await rollbackVersion(currentDocumentId.value, row.id)
    ElMessage.success('回滚成功')
    versionHistoryVisible.value = false
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('回滚失败:', error)
      ElMessage.error('回滚失败')
    }
  }
}

// 刷新统计
const handleRefreshStats = async () => {
  try {
    const stats = await getCategoryDocumentCount()
    // 构建友好的展示信息
    const message = stats.map(item => 
      `${item.categoryName}: ${item.documentCount} 篇`
    ).join('\n')
    
    await ElMessageBox.alert(message, '分类文档数量统计', {
      confirmButtonText: '确定',
      dangerouslyUseHTMLString: false
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 状态映射
const getStatusType = (status) => {
  const map = { DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'warning' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
  return map[status] || status
}

onMounted(() => {
  loadCategories()
  loadDocuments()
})
</script>

<style scoped>
.document-list {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.markdown-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
}
</style>

<style>
/* 引入github-markdown-css样式 */
@import 'github-markdown-css/github-markdown.css';
</style>
