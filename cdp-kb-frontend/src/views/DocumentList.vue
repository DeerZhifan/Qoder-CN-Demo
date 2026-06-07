<template>
  <div class="document-list">
    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
            <el-option label="已删除" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="$router.push('/documents/create')">
        <el-icon><Plus /></el-icon>
        新建文档
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status, row.deleted) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80">
          <template #default="{ row }">
            v{{ row.version }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row.id)">编辑</el-button>
            <el-button
              v-if="row.deleted === 1"
              size="small"
              type="success"
              @click="handleRestore(row.id)"
            >
              恢复
            </el-button>
            <template v-else>
              <el-button
                v-if="row.status !== 1"
                size="small"
                type="success"
                @click="handlePublish(row.id)"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 1"
                size="small"
                type="warning"
                @click="handleOffline(row.id)"
              >
                下线
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(row.id)">
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          @current-change="fetchDocuments"
          @size-change="fetchDocuments"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocuments, deleteDocument, restoreDocument, publishDocument, offlineDocument } from '@/api/document'
import { getCategories } from '@/api/category'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const categories = ref([])

const filters = reactive({
  categoryId: null,
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 获取文档列表
const fetchDocuments = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    
    // 处理删除状态筛选
    if (filters.status === -1) {
      // 这里需要后端支持 deleted 参数，暂时简化
      params.status = null
    } else {
      params.status = filters.status
    }
    
    if (filters.categoryId) {
      params.categoryId = filters.categoryId
    }
    
    const result = await getDocuments(params)
    tableData.value = result.records
    pagination.total = result.total
  } catch (error) {
    console.error('获取文档列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取分类名称
const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat ? cat.name : '-'
}

// 获取状态文本
const getStatusText = (status, deleted) => {
  if (deleted === 1) return '已删除'
  const statusMap = { 0: '草稿', 1: '已发布', 2: '已下线' }
  return statusMap[status] || '未知'
}

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = { 0: 'info', 1: 'success', 2: 'warning' }
  return typeMap[status] || 'info'
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchDocuments()
}

// 重置
const handleReset = () => {
  filters.categoryId = null
  filters.status = null
  handleSearch()
}

// 编辑
const handleEdit = (id) => {
  router.push(`/documents/${id}/edit`)
}

// 删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('删除后可在已删除列表中恢复，确认删除？', '提示', {
      type: 'warning'
    })
    await deleteDocument(id)
    ElMessage.success('删除成功')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 恢复
const handleRestore = async (id) => {
  try {
    await ElMessageBox.confirm('确认恢复该文档？', '提示', {
      type: 'info'
    })
    await restoreDocument(id)
    ElMessage.success('恢复成功')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复失败:', error)
    }
  }
}

// 发布
const handlePublish = async (id) => {
  try {
    await ElMessageBox.confirm('确认发布该文档？发布后前台可见', '提示', {
      type: 'success'
    })
    await publishDocument(id)
    ElMessage.success('发布成功')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发布失败:', error)
    }
  }
}

// 下线
const handleOffline = async (id) => {
  try {
    await ElMessageBox.confirm('确认下线该文档？下线后前台不可见', '提示', {
      type: 'warning'
    })
    await offlineDocument(id)
    ElMessage.success('下线成功')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下线失败:', error)
    }
  }
}

onMounted(() => {
  fetchCategories()
  fetchDocuments()
})
</script>

<style scoped>
.document-list {
  height: 100%;
}

.filter-card {
  margin-bottom: 16px;
}

.action-bar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
