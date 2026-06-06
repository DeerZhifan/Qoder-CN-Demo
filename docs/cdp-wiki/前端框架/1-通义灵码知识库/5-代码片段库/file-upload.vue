<!--
  [场景:文件上传] — 使用 uploadFile API
  源码参考: src/cdp-common-ctrl/components/Upload/
-->
<script setup lang="ts">
import { uploadFile } from "@/cdp-common/utils/request";
import { getTokenHeader } from "@/cdp-common/utils/request";

// 方式一：使用 Element Plus Upload 组件 + 自定义上传
const handleUpload = async (options: any) => {
  const formData = new FormData();
  formData.append("file", options.file);

  try {
    const res = await uploadFile("/file/upload", formData);
    ElMessage.success("上传成功");
    console.log("文件地址:", res.data);
  } catch (error) {
    ElMessage.error("上传失败");
  }
};

// 方式二：使用 Element Plus Upload 组件自带上传（需手动拼接 action URL 和 headers）
const uploadUrl = "/api/file/upload";
const uploadHeaders = getTokenHeader();

const handleSuccess = (response: any) => {
  if (response.code === 200) {
    ElMessage.success("上传成功");
  }
};

const handleError = () => {
  ElMessage.error("上传失败");
};
</script>

<template>
  <div>
    <!-- 方式一：自定义上传（推荐，走统一的 request.ts） -->
    <el-upload
      :http-request="handleUpload"
      :show-file-list="false"
      accept=".xlsx,.xls,.csv"
    >
      <el-button type="primary">自定义上传</el-button>
    </el-upload>

    <!-- 方式二：原生 action 上传 -->
    <el-upload
      :action="uploadUrl"
      :headers="uploadHeaders"
      :on-success="handleSuccess"
      :on-error="handleError"
      :limit="1"
    >
      <el-button type="primary">原生上传</el-button>
    </el-upload>
  </div>
</template>
