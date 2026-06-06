---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/02-module-http-api.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-http-api.md
  - docs/knowledge-base/5-代码片段库/api-module-standard.ts
---

# CDP HTTP/API 层开发规范

## 适用场景

当需要新增 API 接口、调用后端服务、处理文件上传下载、选择请求方法时，必须遵循本规则。适用于所有通过 HTTP 与后端交互的场景。

## 前置依赖

- 集中式 Axios 实例：`src/cdp-common/utils/request.ts`
- 路径别名 `@/` 映射到 `src/`
- 全局类型 `PageQuery`、`PageResult<T>` 定义在 `src/typings/global.d.ts`
- API 聚合文件：`src/_changeable/api.ts`

## 配置要点

### Axios 实例

框架提供两个 Axios 实例，开发者无需自行创建：

| 实例 | 用途 | 超时 | Content-Type |
|------|------|------|-------------|
| `service` | 主业务请求 | 20000ms | application/json |
| `casService` | CAS 单点登录专用 | 20000ms | - |

两个实例共享 `baseURL = import.meta.env.VITE_APP_BASE_API`（开发 `/api/`，生产 `/`）。

### 请求拦截器

所有请求自动经过以下处理，开发者无需干预：

1. **Token 注入** — 根据 `globalConfig.sessionCookie` 从 Cookie 或 localStorage 读取 `cdp-token`，设置到请求头
2. **Request ID** — 自动生成 17 字符唯一标识，设置到 `x-request-id` 头，用于链路追踪
3. **AES 加密** — 当 `VITE_API_ENCRYPT=true` 且 URL 不在白名单中时，自动加密请求数据（FormData 自动跳过）

### 响应拦截器

响应自动经过分层处理：

| 响应码 | 处理方式 |
|--------|---------|
| `code === 200` | 返回 `respData`（成功） |
| `ArrayBuffer / Blob` | 直接返回 `response`（文件下载） |
| `code === 401` | 自动跳转登录页（CAS 模式跳 CAS 登出 URL） |
| `code <= 1000` | `ElMessage.error(msg)` + `Promise.reject`（框架弹错误提示） |
| `code > 1000` | 返回 `respData`（不弹提示，由开发者自行判断处理） |

## 代码模式

### 推荐写法

#### API 模块文件组织

```
cdp-common-frame/{模块名}/
├── api/
│   ├── index.ts      # API 方法定义
│   └── types.ts      # TypeScript 类型定义
└── views/            # 页面组件
```

#### 标准 API 模块写法

**types.ts** — 类型定义：

```typescript
import type { PageQuery, PageResult } from "@/typings/global";

/** 查询参数（继承 PageQuery 获得 pageNum/pageSize） */
export interface ExampleQuery extends PageQuery {
  keyword?: string;
  status?: number;
}

/** 表单数据 */
export interface ExampleForm {
  id?: string;
  name: string;
  description?: string;
  status?: number;
}

/** 列表项 */
export interface ExampleItem {
  id: string;
  name: string;
  description: string;
  status: number;
  createTime: string;
}
```

**index.ts** — API 方法：

```typescript
import { postJson, get, del, uploadFile, getExport } from "@/cdp-common/utils/request";
import type { ExampleQuery, ExampleForm } from "./types";

/** 分页列表 */
export function listExamplePages(params: ExampleQuery) {
  return postJson("/example/page", params);
}

/** 详情 */
export function getExampleDetail(id: string) {
  return get(`/example/${id}/detail`);
}

/** 新增 */
export function addExample(data: ExampleForm) {
  return postJson("/example", data);
}

/** 修改 */
export function updateExample(data: ExampleForm) {
  return postJson("/example/edit", data);
}

/** 删除（支持批量，id 逗号分隔） */
export function deleteExample(ids: string) {
  return del(`/example/${ids}`);
}
```

#### 请求方法选择

| 场景 | 方法 | 说明 |
|------|------|------|
| JSON 数据提交 | `postJson(url, params)` | **最常用**，提交对象必须用此方法 |
| 表单数据提交 | `post(url, params)` | 内部用 `qs.stringify` 序列化，不能传嵌套对象 |
| GET 查询 | `get(url, params)` | 参数自动拼为 query string |
| PUT 修改 | `put(url, params)` | JSON body |
| DELETE 删除 | `del(url, params)` | body 传参 |
| 文件上传 | `uploadFile(url, formData)` | 参数为 FormData |
| 文件导出 | `getExport(url, params)` | responseType: blob，超时 999999ms |
| 文件下载 | `getFile(url, headers)` | responseType: blob |

#### 页面中调用 API

```vue
<script setup lang="ts">
import { listExamplePages, deleteExample } from "@/cdp-common-frame/example/api";

const loading = ref(false);
const tableData = ref([]);

async function loadData() {
  loading.value = true;
  try {
    const { data } = await listExamplePages({ pageNum: 1, pageSize: 20 });
    tableData.value = data.records;
  } finally {
    loading.value = false;
  }
}

async function handleDelete(ids: string) {
  await deleteExample(ids);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => loadData());
</script>
```

#### 文件下载

```typescript
import { getExport } from "@/cdp-common/utils/request";
import { saveAs } from "file-saver";

async function handleExport() {
  const res = await getExport("/example/export", queryParams);
  const blob = new Blob([res.data], { type: "application/vnd.ms-excel" });
  saveAs(blob, "数据导出.xlsx");
}
```

### 禁止事项

1. **禁止直接使用 axios** — 必须使用 `@/cdp-common/utils/request` 中的封装方法，确保 Token 注入和加密逻辑一致
2. **禁止硬编码 baseURL** — baseURL 由环境变量 `VITE_APP_BASE_API` 统一管理，不要在 API 方法中写死域名或端口
3. **禁止单独处理 Token** — Token 由请求拦截器自动注入到 `cdp-token` 请求头，不要手动读取或设置
4. **禁止手动处理 401** — 401 状态码由响应拦截器统一处理（跳转登录页），不要在业务代码中判断
5. **禁止省略 TypeScript 类型** — API 参数和返回值必须定义类型接口，放在模块的 `types.ts` 中
6. **禁止使用 any** — 使用明确的 `interface` 或 `Record<string, unknown>` 替代
7. **禁止在 API 层引入 UI 组件** — API 层只封装 HTTP 请求，不要在其中调用 `ElMessage`、`ElMessageBox` 等 UI 组件
