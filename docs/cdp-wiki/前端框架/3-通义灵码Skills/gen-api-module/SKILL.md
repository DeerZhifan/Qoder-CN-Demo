---
type: gen
description: 基于 CDP 前端框架规范，为指定业务模块生成标准的 API 文件和类型定义文件
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/02-module-http-api.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-http-api.md
  - docs/knowledge-base/5-代码片段库/api-module-standard.ts
---

# 生成 API 模块

## 输入参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| 模块名称 | 是 | 英文，kebab-case | `user-manage` |
| 实体名称 | 是 | 大驼峰 PascalCase | `UserManage` |
| API 端点列表 | 否 | 需要生成的接口（默认生成 CRUD） | `listPage, add, update, delete, export` |
| 实体字段列表 | 否 | 实体的业务字段（默认生成示例字段） | `name: string, status: number, remark?: string` |

## 执行步骤

### 第 1 步：创建类型定义文件 types.ts

生成文件路径：`src/cdp-common-frame/{模块名称}/api/types.ts`

```typescript
import type { PageQuery, PageResult } from "@/typings/global";

/** {实体名称} 查询参数 */
export interface {实体名称}Query extends PageQuery {
  // TODO: 补充查询字段
  keyword?: string;
  status?: number;
}

/** {实体名称} 表单数据 */
export interface {实体名称}Form {
  id?: string;
  // TODO: 补充表单字段
  // {实体字段列表}
}

/** {实体名称} 列表项 */
export interface {实体名称}Item {
  id: string;
  // TODO: 补充列表字段
  // {实体字段列表}
  createTime: string;
}

/** {实体名称} 详情 */
export interface {实体名称}DTO {
  id: string;
  // TODO: 补充详情字段
  // {实体字段列表}
  createTime: string;
  updateTime: string;
}
```

### 第 2 步：创建 API 方法文件 index.ts

生成文件路径：`src/cdp-common-frame/{模块名称}/api/index.ts`

```typescript
import { postJson, get, del, getExport, uploadFile } from "@/cdp-common/utils/request";
import type { {实体名称}Query, {实体名称}Form } from "./types";

/** 分页查询{实体名称}列表 */
export function list{实体名称}Pages(params: {实体名称}Query) {
  return postJson("/{模块名称}/listPage", params);
}

/** 获取{实体名称}详情 */
export function get{实体名称}Detail(id: string) {
  return get(`/{模块名称}/${id}/detail`);
}

/** 新增{实体名称} */
export function add{实体名称}(data: {实体名称}Form) {
  return postJson("/{模块名称}/add", data);
}

/** 修改{实体名称} */
export function update{实体名称}(data: {实体名称}Form) {
  return postJson("/{模块名称}/update", data);
}

/** 删除{实体名称}（支持批量，id 逗号分隔） */
export function delete{实体名称}(ids: string) {
  return del(`/{模块名称}/delete/${ids}`);
}

/** 导出{实体名称} Excel */
export function export{实体名称}(params: {实体名称}Query) {
  return getExport("/{模块名称}/export", params);
}
```

## 完成后提醒

生成完成后，请检查以下事项：

1. **补充实体字段** — 替换 types.ts 中的 `TODO` 注释，根据实际业务填写 Query、Form、Item、DTO 的字段定义
2. **添加非标准端点** — 如果业务需要非 CRUD 的接口（如审批、导入、状态变更），在 index.ts 中补充对应方法
3. **确认后端路径一致** — 核对 index.ts 中的 URL 路径与后端接口文档一致，尤其注意路径前缀
4. **聚合到全局 API** — 如需通过 `$api` 全局访问，将模块导出添加到 `src/_changeable/api.ts`
5. **选择正确的请求方法** — JSON 提交用 `postJson`，表单提交用 `post`，查询用 `get`，删除用 `del`
