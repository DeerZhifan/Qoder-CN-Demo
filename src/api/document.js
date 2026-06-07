import request from '@/utils/request'

/**
 * 分页查询文档列表
 * @param {Object} params - 查询参数
 * @param {Long} params.categoryId - 分类ID
 * @param {String} params.status - 文档状态
 * @param {String} params.title - 文档标题(模糊查询)
 * @param {Integer} params.pageNum - 页码
 * @param {Integer} params.pageSize - 每页数量
 */
export function pageDocuments(params) {
  return request.get('/documents', { params })
}

/**
 * 获取分类树
 */
export function getCategoryTree() {
  return request.get('/categories/tree')
}

/**
 * 获取文档详情
 * @param {Long} id - 文档ID
 */
export function getDocument(id) {
  return request.get(`/documents/${id}`)
}

/**
 * 创建文档草稿
 * @param {Object} data - 文档数据
 * @param {Long} data.categoryId - 分类ID
 * @param {String} data.title - 文档标题
 * @param {String} data.content - 文档内容(Markdown格式)
 */
export function createDocument(data) {
  return request.post('/documents', data)
}

/**
 * 更新文档
 * @param {Long} id - 文档ID
 * @param {Object} data - 更新数据
 * @param {String} data.title - 文档标题
 * @param {String} data.content - 文档内容
 */
export function updateDocument(id, data) {
  return request.put(`/documents/${id}`, data)
}

/**
 * 发布文档
 * @param {Long} id - 文档ID
 */
export function publishDocument(id) {
  return request.post(`/documents/${id}/publish`)
}

/**
 * 下线文档
 * @param {Long} id - 文档ID
 */
export function offlineDocument(id) {
  return request.post(`/documents/${id}/offline`)
}

/**
 * 删除文档
 * @param {Long} id - 文档ID
 */
export function deleteDocument(id) {
  return request.delete(`/documents/${id}`)
}

/**
 * 获取文档版本列表
 * @param {Long} id - 文档ID
 */
export function getVersionList(id) {
  return request.get(`/documents/${id}/versions`)
}

/**
 * 版本回滚
 * @param {Long} documentId - 文档ID
 * @param {Long} versionId - 版本ID
 */
export function rollbackVersion(documentId, versionId) {
  return request.post(`/documents/${documentId}/rollback/${versionId}`)
}
