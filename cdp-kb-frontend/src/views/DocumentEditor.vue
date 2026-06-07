<template>
  <div class="document-editor">
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文档标题" />
        </el-form-item>
        
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="变更说明">
          <el-input v-model="form.changeLog" placeholder="可选，用于版本记录" />
        </el-form-item>
        
        <el-form-item label="正文" prop="content">
          <MdEditor
            v-model="form.content"
            :preview="true"
            height="500px"
            language="zh-CN"
            @onSave="handleSave"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">
            保存草稿
          </el-button>
          <el-button type="success" @click="handlePublish" :loading="publishing">
            保存并发布
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getDocument, createDocument, updateDocument, publishDocument } from '@/api/document'
import { getCategories } from '@/api/category'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const saving = ref(false)
const publishing = ref(false)
const categories = ref([])

const form = reactive({
  id: null,
  title: '',
  categoryId: null,
  content: '',
  changeLog: ''
})

const rules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '分类不能为空', trigger: 'change' }
  ],
  content: [
    { required: true, message: '正文不能为空', trigger: 'blur' }
  ]
}

const isEdit = ref(false)

// 获取分类列表
const fetchCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 加载文档（编辑模式）
const loadDocument = async (id) => {
  try {
    const doc = await getDocument(id)
    form.id = doc.id
    form.title = doc.title
    form.categoryId = doc.categoryId
    form.content = doc.content
  } catch (error) {
    console.error('加载文档失败:', error)
    ElMessage.error('加载文档失败')
  }
}

// 保存草稿
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.warning('请填写必填项（标题、分类、正文）')
    return
  }
  
  saving.value = true
  try {
    const data = {
      title: form.title,
      categoryId: form.categoryId,
      content: form.content,
      status: 0, // 草稿状态
      changeLog: form.changeLog
    }
    
    if (isEdit.value) {
      await updateDocument(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createDocument(data)
      ElMessage.success('创建成功')
    }
    
    router.push('/documents')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 保存并发布
const handlePublish = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.warning('请填写必填项（标题、分类、正文）')
    return
  }
  
  publishing.value = true
  try {
    let docId = form.id
    
    // 如果是新建，先保存
    if (!isEdit.value) {
      const data = {
        title: form.title,
        categoryId: form.categoryId,
        content: form.content,
        status: 0,
        changeLog: form.changeLog
      }
      const newDoc = await createDocument(data)
      docId = newDoc.id
    } else {
      // 如果是编辑，先更新
      const data = {
        title: form.title,
        categoryId: form.categoryId,
        content: form.content,
        changeLog: form.changeLog
      }
      await updateDocument(docId, data)
    }
    
    // 发布
    await publishDocument(docId)
    ElMessage.success('发布成功')
    router.push('/documents')
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    publishing.value = false
  }
}

// 取消
const handleCancel = () => {
  router.push('/documents')
}

onMounted(async () => {
  await fetchCategories()
  
  if (route.params.id) {
    isEdit.value = true
    await loadDocument(route.params.id)
  }
})
</script>

<style scoped>
.document-editor {
  max-width: 1200px;
}
</style>
