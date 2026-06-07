import request from '@/utils/request'

// 获取文档列表（分页）
export function getDocuments(params) {
  return request({
    url: '/documents',
    method: 'get',
    params
  })
}

// 获取文档详情
export function getDocument(id) {
  return request({
    url: `/documents/${id}`,
    method: 'get'
  })
}

// 创建文档
export function createDocument(data) {
  return request({
    url: '/documents',
    method: 'post',
    data
  })
}

// 更新文档
export function updateDocument(id, data) {
  return request({
    url: `/documents/${id}`,
    method: 'put',
    data
  })
}

// 删除文档
export function deleteDocument(id) {
  return request({
    url: `/documents/${id}`,
    method: 'delete'
  })
}

// 恢复文档
export function restoreDocument(id) {
  return request({
    url: `/documents/${id}/restore`,
    method: 'post'
  })
}

// 发布文档
export function publishDocument(id) {
  return request({
    url: `/documents/${id}/publish`,
    method: 'post'
  })
}

// 下线文档
export function offlineDocument(id) {
  return request({
    url: `/documents/${id}/offline`,
    method: 'post'
  })
}

// 获取版本列表
export function getVersions(documentId) {
  return request({
    url: `/documents/${documentId}/versions`,
    method: 'get'
  })
}

// 获取指定版本
export function getVersion(documentId, version) {
  return request({
    url: `/documents/${documentId}/versions/${version}`,
    method: 'get'
  })
}
