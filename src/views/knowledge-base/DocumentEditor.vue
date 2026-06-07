<template>
  <div class="document-editor">
    <el-card>
      <el-row :gutter="20">
        <!-- 左侧表单 -->
        <el-col :span="7">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
            </el-form-item>
            
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="{ label: 'name', children: 'children' }"
                node-key="id"
                placeholder="请选择分类"
                check-strictly
              />
            </el-form-item>
            
            <el-form-item label="状态">
              <el-tag :type="getStatusType(form.status)">{{ getStatusText(form.status) }}</el-tag>
            </el-form-item>
            
            <el-form-item v-if="form.publishTime" label="发布时间">
              <span>{{ form.publishTime }}</span>
            </el-form-item>
          </el-form>
        </el-col>

        <!-- 右侧编辑器 -->
        <el-col :span="17">
          <MdEditor
            v-model="form.content"
            :toolbars="toolbars"
            height="600px"
            preview-theme="github"
          />
        </el-col>
      </el-row>

      <!-- 底部按钮 -->
      <div class="button-group">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSaveDraft" :loading="saving">保存草稿</el-button>
        <el-button v-if="form.status === 'DRAFT'" type="success" @click="handlePublish" :loading="publishing">发布</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getDocument, createDocument, updateDocument, publishDocument } from '@/api/document'
import { getCategoryTree } from '@/api/category'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)

// 表单数据
const form = reactive({
  id: null,
  categoryId: null,
  title: '',
  content: '',
  status: 'DRAFT',
  publishTime: null
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过200个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ]
}

// 状态
const categoryTree = ref([])
const saving = ref(false)
const publishing = ref(false)

// 编辑器工具栏配置
const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'quote', 'unorderedList', 'orderedList', '-',
  'codeRow', 'code', 'link', 'image', 'table', '-',
  'preview', 'fullscreen'
]

// 加载分类树
const loadCategoryTree = async () => {
  try {
    categoryTree.value = await getCategoryTree()
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

// 加载文档详情(编辑模式)
const loadDocument = async (id) => {
  try {
    const doc = await getDocument(id)
    Object.assign(form, doc)
  } catch (error) {
    ElMessage.error('加载文档失败')
    router.back()
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    
    const data = {
      categoryId: form.categoryId,
      title: form.title,
      content: form.content
    }
    
    if (form.id) {
      await updateDocument(form.id, data)
      ElMessage.success('保存成功')
    } else {
      const newId = await createDocument(data)
      form.id = newId
      ElMessage.success('创建成功')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

// 发布
const handlePublish = async () => {
  try {
    await formRef.value.validate()
    
    // 先保存草稿
    if (!form.id) {
      const data = {
        categoryId: form.categoryId,
        title: form.title,
        content: form.content
      }
      form.id = await createDocument(data)
    }
    
    publishing.value = true
    await publishDocument(form.id)
    ElMessage.success('发布成功')
    router.push('/knowledge-base/list')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    publishing.value = false
  }
}

// 取消
const handleCancel = () => {
  router.back()
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

onMounted(async () => {
  await loadCategoryTree()
  
  const id = route.params.id
  if (id && id !== 'new') {
    await loadDocument(id)
  }
})
</script>

<style scoped>
.document-editor {
  padding: 20px;
}
.button-group {
  margin-top: 20px;
  text-align: right;
}
</style>
