<!--
  [场景:表单校验] — Element Plus 表单 + rules 定义
-->
<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";

interface FormData {
  name: string;
  email: string;
  phone: string;
  status: number;
  description: string;
}

const formRef = ref<FormInstance>();

const formData = reactive<FormData>({
  name: "",
  email: "",
  phone: "",
  status: 1,
  description: "",
});

const rules: FormRules<FormData> = {
  name: [
    { required: true, message: "请输入名称", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2-50 个字符", trigger: "blur" },
  ],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱格式", trigger: "blur" },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号", trigger: "blur" },
  ],
};

async function handleSubmit() {
  // 表单校验
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  // 提交逻辑
  console.log("提交数据:", formData);
  ElMessage.success("提交成功");
}

function handleReset() {
  formRef.value?.resetFields();
}
</script>

<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="100px"
  >
    <el-form-item label="名称" prop="name">
      <el-input v-model="formData.name" placeholder="请输入名称" />
    </el-form-item>

    <el-form-item label="邮箱" prop="email">
      <el-input v-model="formData.email" placeholder="请输入邮箱" />
    </el-form-item>

    <el-form-item label="手机号" prop="phone">
      <el-input v-model="formData.phone" placeholder="请输入手机号" />
    </el-form-item>

    <el-form-item label="状态" prop="status">
      <el-radio-group v-model="formData.status">
        <el-radio :label="1">启用</el-radio>
        <el-radio :label="0">禁用</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="描述" prop="description">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="3"
        placeholder="请输入描述"
      />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit">提交</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>
