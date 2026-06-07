<template>
  <div class="category-manage">
    <el-card>
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="categories" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column label="父分类" width="150">
          <template #default="{ row }">
            {{ row.parentId === 0 ? '根分类' : getParentName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option label="根分类" :value="0" />
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
              :disabled="isEdit && form.id === cat.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

const formRef = ref(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const categories = ref([])
const isEdit = ref(false)

const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  sortOrder: 0
})

const rules = {
  name: [
    { required: true, message: '分类名称不能为空', trigger: 'blur' }
  ]
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取父分类名称
const getParentName = (parentId) => {
  const cat = categories.value.find(c => c.id === parentId)
  return cat ? cat.name : '-'
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    parentId: 0,
    sortOrder: 0
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    sortOrder: row.sortOrder
  })
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    const data = {
      name: form.name,
      parentId: form.parentId,
      sortOrder: form.sortOrder
    }
    
    if (isEdit.value) {
      await updateCategory(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createCategory(data)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    fetchCategories()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该分类？如果该分类下有文档则无法删除', '提示', {
      type: 'warning'
    })
    await deleteCategory(id)
    ElMessage.success('删除成功')
    fetchCategories()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-manage {
  max-width: 1200px;
}

.action-bar {
  margin-bottom: 16px;
}
</style>
