# 如何使用 CDP Web HTTP/API 层

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: API, Axios, 请求, postJson, post, get, 接口调用, 文件上传, 文件下载, request.ts

---

## 概述

CDP Web 通过集中式 Axios 实例（`src/cdp-common/utils/request.ts`）统一管理所有 HTTP 请求，提供 16 种请求方法、自动 Token 注入、AES 加解密、请求链路追踪等能力。所有 API 模块通过 `src/_changeable/api.ts` 聚合后挂载为全局属性 `$api`。

## 新增 API 模块的步骤

### 第 1 步：创建类型定义文件

在业务模块目录下创建 `api/模块名/types.ts`：

```typescript
// src/cdp-common-frame/system/api/user/types.ts
export interface UserQuery {
  username?: string;
  status?: number;
}

export interface UserForm {
  id?: number;
  username: string;
  nickname: string;
  roleIds: number[];
}
```

### 第 2 步：创建 API 模块

创建 `api/模块名/index.ts`，导入 request 工具：

```typescript
// src/cdp-common-frame/system/api/user/index.ts
import * as request from "@/cdp-common/utils/request";
import { UserQuery, UserForm } from "./types";

// 分页查询用户列表
export function listUsers(params: UserQuery) {
  return request.postJson("/system/user/listPage", params);
}

// 新增用户
export function addUser(data: UserForm) {
  return request.postJson("/system/user/add", data);
}

// 修改用户
export function updateUser(data: UserForm) {
  return request.postJson("/system/user/update", data);
}

// 删除用户
export function deleteUser(ids: string) {
  return request.post(`/system/user/delete/${ids}`);
}
```

### 第 3 步：聚合到全局 API

在模块的 `api/all.ts` 中导出，最终通过 `src/_changeable/api.ts` 聚合：

```typescript
// src/_changeable/api.ts
import { cdpFrameApi } from "@/cdp-common-frame";
import { cdpAdminApi } from "@/cdp-common-admin";

export default {
  ...cdpFrameApi,
  ...cdpAdminApi,
};
```

聚合后即可在组件中通过 `$api` 调用。

## 请求方法选择指南

| 场景 | 方法 | Content-Type | 说明 |
|------|------|-------------|------|
| JSON 数据提交 | `postJson(url, params)` | application/json | **最常用**，提交对象必须用此方法 |
| 表单数据提交 | `post(url, params)` | x-www-form-urlencoded | 内部用 `qs.stringify` 序列化 |
| URL 参数 POST | `postJsonParams(url, params)` | application/json | 参数拼到 URL，body 为空 |
| GET 请求 | `get(url, params)` | - | 参数自动拼为 query string |
| PUT 请求 | `put(url, params)` | application/json | JSON body |
| DELETE 请求 | `del(url, params)` | - | body 传参 |
| 文件上传 | `uploadFile(url, formData)` | multipart/form-data | 参数为 FormData |
| 上传并返回文件 | `uploadImportFile(url, formData)` | multipart/form-data | responseType: blob |
| 文件导出（POST） | `getExport(url, params)` | x-www-form-urlencoded | responseType: blob，超时 999999ms |
| 文件下载（GET） | `getFile(url, headers)` | - | responseType: blob |
| 文件下载（POST+body） | `postFile(url, params)` | multipart/form-data | responseType: blob |
| SSE 流式 | `getSSE(url, params)` | - | responseType: stream |

## 在组件中调用 API

### 方式一：通过 $api 全局属性

```vue
<script setup lang="ts">
import { getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance()!;

// 调用已聚合的 API
const res = await proxy!.$api.listUsers({ username: "admin" });
</script>
```

### 方式二：直接导入 API 函数（推荐）

```vue
<script setup lang="ts">
import { listUsers, deleteUser } from "@/cdp-common-frame/system/api/user";

const loadData = async () => {
  const { data } = await listUsers({ username: "" });
  tableData.value = data.records;
};
</script>
```

## 文件上传示例

```typescript
import { uploadFile } from "@/cdp-common/utils/request";

const formData = new FormData();
formData.append("file", file);
formData.append("bizType", "avatar");
const res = await uploadFile("/system/file/upload", formData);
```

## 文件下载示例

```typescript
import { getExport } from "@/cdp-common/utils/request";
import { saveAs } from "file-saver";

const res = await getExport("/system/user/export", queryParams);
const blob = new Blob([res.data], { type: "application/vnd.ms-excel" });
saveAs(blob, "用户列表.xlsx");
```

## 注意事项

> 注意：提交 JSON 对象必须使用 `postJson()`，不要使用 `post()`。`post()` 会用 `qs.stringify` 序列化数据为表单格式，导致后端无法正确解析嵌套对象。

> 注意：`FormData` 类型的数据（如文件上传）会自动跳过 AES 加密，无需额外处理。

> 注意：所有请求自动携带 `cdp-token` 请求头和 `x-request-id` 链路追踪 ID，无需手动添加。

> 注意：响应状态码 `200` 表示成功；`401` 自动跳转登录页；`code > 1000` 不弹报错提示，由业务代码自行处理。

> 注意：不要自行创建新的 Axios 实例，统一使用 `request.ts` 中的方法，确保 Token 注入和加密逻辑一致。
