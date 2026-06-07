<template>
  <div class="category-tree-manager">
    <div class="tree-header">
      <h3>分类管理</h3>
      <el-button type="primary" size="small" @click="showAddDialog(0)">新增根分类</el-button>
    </div>
    
    <el-tree
      :data="categoryTree"
      :props="{ label: 'name', children: 'children' }"
      node-key="id"
      default-expand-all
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span>{{ node.label }}</span>
          <span class="tree-actions">
            <el-button link size="small" @click.stop="showAddDialog(data.id)">新增子分类</el-button>
            <el-button link size="small" @click.stop="showEditDialog(data)">编辑</el-button>
            <el-button link size="small" type="danger" @click.stop="handleDelete(data.id)">删除</el-button>
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form :model="categoryForm" :rules="rules" ref="formRef">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/category'

const emit = defineEmits(['refresh'])

const categoryTree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref(null)
const isEdit = ref(false)

const categoryForm = reactive({
  id: null,
  parentId: 0,
  name: '',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

// 加载分类树
const loadTree = async () => {
  try {
    categoryTree.value = await getCategoryTree()
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

// 显示新增对话框
const showAddDialog = (parentId) => {
  isEdit.value = false
  dialogTitle.value = '新增分类'
  categoryForm.parentId = parentId
  categoryForm.name = ''
  categoryForm.sortOrder = 0
  dialogVisible.value = true
}

// 显示编辑对话框
const showEditDialog = (data) => {
  isEdit.value = true
  dialogTitle.value = '编辑分类'
  categoryForm.id = data.id
  categoryForm.parentId = data.parentId
  categoryForm.name = data.name
  categoryForm.sortOrder = data.sortOrder
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (isEdit.value) {
      await updateCategory(categoryForm.id, {
        name: categoryForm.name,
        sortOrder: categoryForm.sortOrder
      })
      ElMessage.success('更新成功')
    } else {
      await createCategory({
        parentId: categoryForm.parentId,
        name: categoryForm.name,
        sortOrder: categoryForm.sortOrder
      })
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    await loadTree()
    emit('refresh')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

// 删除分类
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该分类?', '警告', { type: 'warning' })
    await deleteCategory(id)
    ElMessage.success('删除成功')
    await loadTree()
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTree()
})

defineExpose({ loadTree })
</script>

<style scoped>
.category-tree-manager {
  padding: 10px;
}
.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.tree-header h3 {
  margin: 0;
  font-size: 16px;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.tree-actions {
  opacity: 0;
  transition: opacity 0.2s;
}
.el-tree-node__content:hover .tree-actions {
  opacity: 1;
}
</style>
