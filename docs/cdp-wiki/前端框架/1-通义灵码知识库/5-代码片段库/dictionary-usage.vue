<!--
  [场景:字典数据使用] — 字典选择器和字典值显示
  源码参考: src/cdp-common-ctrl/components/Dictionary/
-->
<script setup lang="ts">
import { get } from "@/cdp-common/utils/request";

// 字典数据
const statusOptions = ref<{ label: string; value: any }[]>([]);

/** 加载字典数据 */
async function loadDictionary(dictCode: string) {
  const { data } = await get(`/system/dictionary-value/list/${dictCode}`);
  return (data || []).map((item: any) => ({
    label: item.label,
    value: item.value,
  }));
}

onMounted(async () => {
  statusOptions.value = await loadDictionary("sys_status");
});

// 根据字典值获取标签
function getDictLabel(options: any[], value: any): string {
  const item = options.find((opt) => opt.value == value);
  return item?.label || String(value);
}
</script>

<template>
  <div>
    <!-- 表单中使用字典下拉 -->
    <el-form-item label="状态">
      <el-select v-model="formData.status" placeholder="请选择">
        <el-option
          v-for="item in statusOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>

    <!-- 表格中显示字典标签 -->
    <el-table-column prop="status" label="状态">
      <template #default="{ row }">
        {{ getDictLabel(statusOptions, row.status) }}
      </template>
    </el-table-column>
  </div>
</template>
