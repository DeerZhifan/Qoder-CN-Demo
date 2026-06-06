/**
 * [场景:文件下载/导出] — getExport + getFile + Blob 处理
 *
 * 源码参考: src/cdp-common/utils/request.ts
 */

import { getExport, getFile, postFile } from "@/cdp-common/utils/request";

/**
 * 方式一：POST 表单参数导出（最常用）
 * 适用: 导出 Excel、PDF 等，参数通过表单提交
 */
export async function exportExcel(params: any) {
  const res = await getExport("/api/export/excel", params);
  downloadBlob(res.data, "导出数据.xlsx");
}

/**
 * 方式二：GET 方式下载文件
 * 适用: 下载已存在的文件（通过文件 ID 或路径）
 */
export async function downloadFile(fileId: string) {
  const res = await getFile(`/api/file/download/${fileId}`);
  // 从 Content-Disposition 获取文件名
  const fileName = getFileNameFromResponse(res) || "下载文件";
  downloadBlob(res.data, fileName);
}

/**
 * 方式三：POST body 下载
 * 适用: 复杂查询条件的文件导出
 */
export async function exportWithBody(params: any) {
  const res = await postFile("/api/export/complex", params);
  downloadBlob(res.data, "报告.pdf");
}

// ==================== 工具函数 ====================

/**
 * Blob 下载为文件
 */
function downloadBlob(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/**
 * 从响应头获取文件名
 */
function getFileNameFromResponse(response: any): string {
  const disposition = response.headers["content-disposition"];
  if (disposition) {
    const match = disposition.match(/filename\*?=(?:UTF-8'')?(.+)/i);
    if (match) {
      return decodeURIComponent(match[1].replace(/["']/g, ""));
    }
  }
  return "";
}
