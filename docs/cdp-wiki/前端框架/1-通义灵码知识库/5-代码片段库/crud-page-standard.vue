<!--
  [场景:CRUD页面] — 标准增删改查页面模板
  包含: 搜索栏 + 工具栏 + 数据表格 + 分页 + 新增/编辑对话框
  源码参考: src/cdp-common-frame/system/views/user/
-->
<script setup lang="ts">
import {
  listExamplePages,
  addExample,
  updateExample,
  deleteExample,
} from "../api";
import type { ExampleQuery, ExampleForm, ExampleItem } from "../api/types";

// ==================== 查询 ====================
const queryParams = reactive<ExampleQuery>({
  pageNum: 1,
  pageSize: 10,
});
const tableData = ref<ExampleItem[]>([]);
const total = ref(0);
const loading = ref(false);

async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await listExamplePages(queryParams);
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  queryParams.keyword = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  handleQuery();
}

// ==================== 新增/编辑 ====================
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref();
const formData = reactive<ExampleForm>({
  name: "",
  description: "",
});

const rules = {
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
};

function handleAdd() {
  dialogTitle.value = "新增";
  Object.assign(formData, { id: undefined, name: "", description: "" });
  dialogVisible.value = true;
}

function handleEdit(row: ExampleItem) {
  dialogTitle.value = "编辑";
  Object.assign(formData, { ...row });
  dialogVisible.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (formData.id) {
    await updateExample(formData);
  } else {
    await addExample(formData);
  }
  ElMessage.success("操作成功");
  dialogVisible.value = false;
  handleQuery();
}

// ==================== 删除 ====================
async function handleDelete(row: ExampleItem) {
  await ElMessageBox.confirm("确认删除该记录？", "提示", { type: "warning" });
  await deleteExample(row.id);
  ElMessage.success("删除成功");
  handleQuery();
}

// ==================== 初始化 ====================
onMounted(() => handleQuery());
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" class="mb-4">
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入关键词"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="mb-4">
      <el-button v-hasPerm="['example:add']" type="primary" @click="handleAdd">
        新增
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? "启用" : "禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-hasPerm="['example:edit']"
            type="primary"
            link
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-hasPerm="['example:delete']"
            type="danger"
            link
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      v-if="total > 0"
      v-model:total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="handleQuery"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入描述"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.mb-4 {
  margin-bottom: 16px;
}
</style>
