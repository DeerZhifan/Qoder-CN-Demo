<!--
  [场景:分页表格] — 使用 PageQuery/PageResult 标准分页模式
  配合 CdpTable 或 el-table + Pagination 组件
-->
<script setup lang="ts">
import { listExamplePages } from "../api";
import type { ExampleQuery, ExampleItem } from "../api/types";

// 查询参数（继承 PageQuery: pageNum + pageSize）
const queryParams = reactive<ExampleQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
});

// 表格数据
const tableData = ref<ExampleItem[]>([]);
const total = ref(0);
const loading = ref(false);

// 多选
const selectedIds = ref<string[]>([]);

/** 查询 */
async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await listExamplePages(queryParams);
    // data 结构: PageResult<ExampleItem> = { list: [...], total: 100 }
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

/** 重置查询 */
function handleReset() {
  queryParams.keyword = "";
  queryParams.pageNum = 1;
  handleQuery();
}

/** 多选变化 */
function handleSelectionChange(selection: ExampleItem[]) {
  selectedIds.value = selection.map((item) => item.id);
}

onMounted(() => handleQuery());
</script>

<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :inline="true">
      <el-form-item>
        <el-input v-model="queryParams.keyword" placeholder="搜索" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
    </el-table>

    <!-- 分页（全局注册的 Pagination 组件） -->
    <Pagination
      v-if="total > 0"
      v-model:total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="handleQuery"
    />
  </div>
</template>
