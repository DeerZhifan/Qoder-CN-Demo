<template>
  <el-dialog v-model="visible" title="版本历史" width="70%" @close="handleClose">
    <el-table :data="versions" v-loading="loading" border>
      <el-table-column prop="version" label="版本号" width="100" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column prop="createBy" label="创建人" width="120" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="showVersionDetail(row)">查看</el-button>
          <el-button size="small" type="warning" @click="handleRollback(row)">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 版本详情Dialog -->
    <el-dialog v-model="detailVisible" title="版本详情" width="60%" append-to-body>
      <div class="markdown-body" v-html="renderedContent"></div>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { getVersionList, rollbackVersion } from '@/api/document'

const props = defineProps({
  documentId: { type: Number, required: true },
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'close', 'refresh'])

const md = new MarkdownIt()
const visible = ref(props.modelValue)
const loading = ref(false)
const versions = ref([])
const detailVisible = ref(false)
const renderedContent = ref('')

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.documentId) {
    loadVersions()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载版本列表
const loadVersions = async () => {
  loading.value = true
  try {
    versions.value = await getVersionList(props.documentId)
  } catch (error) {
    ElMessage.error('加载版本失败')
  } finally {
    loading.value = false
  }
}

// 查看版本详情
const showVersionDetail = (version) => {
  renderedContent.value = md.render(version.content || '# 暂无内容')
  detailVisible.value = true
}

// 回滚版本
const handleRollback = async (version) => {
  try {
    await ElMessageBox.confirm(`确认回滚到版本${version.version}?`, '警告', { type: 'warning' })
    await rollbackVersion(props.documentId, version.id)
    ElMessage.success('回滚成功')
    emit('refresh')
    handleClose()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('回滚失败')
    }
  }
}

const handleClose = () => {
  visible.value = false
  emit('close')
}
</script>

<style scoped>
.markdown-body {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
