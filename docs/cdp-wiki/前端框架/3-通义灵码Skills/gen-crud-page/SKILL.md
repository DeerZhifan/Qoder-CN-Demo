---
type: gen
description: 基于 CDP 前端框架规范，生成标准 CRUD 页面（含搜索栏+表格+分页+新增/编辑弹窗+删除确认）
rule: cdp-rule-business-module.md
reference:
  - docs/knowledge-base/5-代码片段库/crud-page-standard.vue
  - docs/knowledge-base/5-代码片段库/table-with-pagination.vue
---

# 生成标准 CRUD 页面

## 输入参数

| 参数 | 说明 | 示例 |
|------|------|------|
| {模块名称} | 模块英文名（kebab-case） | `order`、`contract-manage` |
| {实体名称} | 实体英文名（PascalCase，用于类型命名） | `Order`、`Contract` |
| {字段列表} | 业务字段定义 | 见下方格式 |

### 字段列表格式

每个字段包含以下属性：

| 属性 | 说明 | 示例 |
|------|------|------|
| 字段名 | 英文字段名（camelCase） | `orderNo`、`customerName` |
| 类型 | TypeScript 类型 | `string`、`number`、`boolean` |
| 中文名 | 表格列标题 / 表单标签 | `订单号`、`客户名称` |
| 是否搜索条件 | 是否出现在搜索栏 | `true` / `false` |
| 是否表格列 | 是否出现在表格中 | `true` / `false` |
| 是否表单字段 | 是否出现在新增/编辑弹窗中 | `true` / `false` |
| 是否必填 | 表单验证是否必填 | `true` / `false` |
| 表单控件类型 | 表单输入控件类型 | `input`、`select`、`date`、`number`、`textarea` |

示例输入：

```
模块名称: order
实体名称: Order
字段列表:
- orderNo / string / 订单号 / 搜索:是 / 表格:是 / 表单:是 / 必填:是 / 控件:input
- customerName / string / 客户名称 / 搜索:是 / 表格:是 / 表单:是 / 必填:是 / 控件:input
- amount / number / 金额 / 搜索:否 / 表格:是 / 表单:是 / 必填:是 / 控件:number
- status / number / 状态 / 搜索:是 / 表格:是 / 表单:是 / 必填:否 / 控件:select
- remark / string / 备注 / 搜索:否 / 表格:否 / 表单:是 / 必填:否 / 控件:textarea
- createTime / string / 创建时间 / 搜索:否 / 表格:是 / 表单:否
```

## 执行步骤

### 第 1 步：创建页面文件

在 `src/cdp-common-frame/{模块名称}/views/` 下创建 `.vue` 文件，使用 `<script setup lang="ts">` 语法。

### 第 2 步：生成搜索栏区域

根据字段列表中标记为"搜索:是"的字段，生成 `el-form` 搜索栏：

```vue
<!-- 搜索栏 -->
<el-form :inline="true" :model="queryParams" class="mb-4">
  <!-- 为每个搜索字段生成对应控件 -->
  <el-form-item label="{中文名}">
    <!-- input 类型 -->
    <el-input v-model="queryParams.{字段名}" placeholder="请输入{中文名}" clearable @keyup.enter="handleQuery" />
    <!-- select 类型 -->
    <el-select v-model="queryParams.{字段名}" placeholder="全部" clearable>
      <el-option label="启用" :value="1" />
      <el-option label="禁用" :value="0" />
    </el-select>
    <!-- date 类型 -->
    <el-date-picker v-model="queryParams.{字段名}" type="date" placeholder="选择日期" clearable />
  </el-form-item>
  <el-form-item>
    <el-button type="primary" @click="handleQuery">搜索</el-button>
    <el-button @click="handleReset">重置</el-button>
  </el-form-item>
</el-form>
```

### 第 3 步：生成表格区域（含分页）

根据字段列表中标记为"表格:是"的字段，生成 `el-table` + `Pagination`：

```vue
<!-- 工具栏 -->
<div class="mb-4">
  <el-button v-hasPerm="['{模块名称}:add']" type="primary" @click="handleAdd">新增</el-button>
</div>

<!-- 数据表格 -->
<el-table v-loading="loading" :data="tableData" border>
  <!-- 为每个表格字段生成列 -->
  <el-table-column prop="{字段名}" label="{中文名}" min-width="150" />

  <!-- status 字段使用 el-tag 展示 -->
  <el-table-column prop="status" label="状态" width="100">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'">
        {{ row.status === 1 ? "启用" : "禁用" }}
      </el-tag>
    </template>
  </el-table-column>

  <!-- 时间字段固定宽度 -->
  <el-table-column prop="createTime" label="创建时间" width="180" />

  <!-- 操作列 -->
  <el-table-column label="操作" width="200" fixed="right">
    <template #default="{ row }">
      <el-button v-hasPerm="['{模块名称}:edit']" type="primary" link @click="handleEdit(row)">编辑</el-button>
      <el-button v-hasPerm="['{模块名称}:delete']" type="danger" link @click="handleDelete(row)">删除</el-button>
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
```

### 第 4 步：生成新增/编辑弹窗

根据字段列表中标记为"表单:是"的字段，生成 `el-dialog` + `el-form`：

```vue
<!-- 新增/编辑对话框 -->
<el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
    <!-- input 控件 -->
    <el-form-item label="{中文名}" prop="{字段名}">
      <el-input v-model="formData.{字段名}" placeholder="请输入{中文名}" />
    </el-form-item>
    <!-- number 控件 -->
    <el-form-item label="{中文名}" prop="{字段名}">
      <el-input-number v-model="formData.{字段名}" :min="0" :precision="2" />
    </el-form-item>
    <!-- select 控件 -->
    <el-form-item label="{中文名}" prop="{字段名}">
      <el-select v-model="formData.{字段名}" placeholder="请选择{中文名}">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
    </el-form-item>
    <!-- textarea 控件 -->
    <el-form-item label="{中文名}" prop="{字段名}">
      <el-input v-model="formData.{字段名}" type="textarea" placeholder="请输入{中文名}" :rows="3" />
    </el-form-item>
    <!-- date 控件 -->
    <el-form-item label="{中文名}" prop="{字段名}">
      <el-date-picker v-model="formData.{字段名}" type="date" placeholder="选择{中文名}" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit">确定</el-button>
  </template>
</el-dialog>
```

表单验证规则（为必填字段生成 rules）：

```typescript
const rules = {
  // 为每个"必填:是"的字段生成规则
  {字段名}: [{ required: true, message: "请输入{中文名}", trigger: "blur" }],
};
```

### 第 5 步：生成删除确认

```typescript
async function handleDelete(row: {Entity}VO) {
  await ElMessageBox.confirm("确认删除该记录？", "提示", { type: "warning" });
  await delete{Entity}(row.id);
  ElMessage.success("删除成功");
  handleQuery();
}
```

## 完整代码模板

以下为完整的 CRUD 页面生成模板，将各步骤组合：

```vue
<script setup lang="ts">
import {
  list{Entity}Pages,
  add{Entity},
  update{Entity},
  delete{Entity},
} from "../api/{实体名称小写}";
import type {
  {Entity}Query,
  {Entity}Form,
  {Entity}VO,
} from "../api/{实体名称小写}/types";

// ==================== 查询 ====================
const queryParams = reactive<{Entity}Query>({
  pageNum: 1,
  pageSize: 20,
});
const tableData = ref<{Entity}VO[]>([]);
const total = ref(0);
const loading = ref(false);

async function handleQuery() {
  loading.value = true;
  try {
    const { data } = await list{Entity}Pages(queryParams);
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  // 重置所有搜索条件字段为 undefined
  queryParams.pageNum = 1;
  handleQuery();
}

// ==================== 新增/编辑 ====================
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref();
const formData = reactive<{Entity}Form>({
  // 初始化所有表单字段
});

const rules = {
  // 为必填字段生成校验规则
};

function handleAdd() {
  dialogTitle.value = "新增";
  // 重置 formData 所有字段
  dialogVisible.value = true;
}

function handleEdit(row: {Entity}VO) {
  dialogTitle.value = "编辑";
  Object.assign(formData, { ...row });
  dialogVisible.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (formData.id) {
    await update{Entity}(formData);
  } else {
    await add{Entity}(formData);
  }
  ElMessage.success("操作成功");
  dialogVisible.value = false;
  handleQuery();
}

// ==================== 删除 ====================
async function handleDelete(row: {Entity}VO) {
  await ElMessageBox.confirm("确认删除该记录？", "提示", { type: "warning" });
  await delete{Entity}(row.id);
  ElMessage.success("删除成功");
  handleQuery();
}

// ==================== 初始化 ====================
onMounted(() => handleQuery());
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏：根据字段列表中搜索:是的字段生成 -->
    <el-form :inline="true" :model="queryParams" class="mb-4">
      <!-- {搜索字段} -->
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="mb-4">
      <el-button v-hasPerm="['{模块名称}:add']" type="primary" @click="handleAdd">
        新增
      </el-button>
    </div>

    <!-- 数据表格：根据字段列表中表格:是的字段生成 -->
    <el-table v-loading="loading" :data="tableData" border>
      <!-- {表格列} -->
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPerm="['{模块名称}:edit']" type="primary" link @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button v-hasPerm="['{模块名称}:delete']" type="danger" link @click="handleDelete(row)">
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

    <!-- 新增/编辑对话框：根据字段列表中表单:是的字段生成 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <!-- {表单字段} -->
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
```

## 完成后提醒

- 生成的页面需配合对应的 API 模块（`api/{实体名称}/index.ts` + `types.ts`），如尚未创建请先使用 `init-frontend-module` 技能初始化模块
- `status` 类型的字段需根据实际业务调整 `el-option` 选项值和标签
- `select` 控件的选项如需从后端获取，应在 `onMounted` 中调用字典接口加载
- 表单验证规则（`rules`）可根据业务需求补充自定义校验器
- 权限标识（如 `{模块名称}:add`）需与后端权限配置一致
- `Pagination` 为全局注册组件，无需手动导入
- `ElMessage`、`ElMessageBox` 由 unplugin-auto-import 自动导入，无需手动引用
- 生成后运行 `pnpm run lint:eslint` 检查代码规范
- 新增 `.vue` 文件后需重启开发服务器（`pnpm run dev`）
