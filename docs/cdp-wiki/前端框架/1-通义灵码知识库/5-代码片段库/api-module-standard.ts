/**
 * [场景:新增API接口] — API 模块标准写法
 *
 * 文件结构:
 *   模块名/api/types.ts   ← 类型定义
 *   模块名/api/index.ts   ← API 方法
 *
 * 源码参考: src/cdp-common-frame/common/api/login/
 */

// ============================================================
// types.ts — 类型定义
// ============================================================

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

// ============================================================
// index.ts — API 方法
// ============================================================

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

/** 导出 Excel */
export function exportExample(params: ExampleQuery) {
  return getExport("/example/export", params);
}

/** 导入 Excel */
export function importExample(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return uploadFile("/example/import", formData);
}
