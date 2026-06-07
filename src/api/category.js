import request from '@/utils/request'

/**
 * 获取分类树形结构
 */
export function getCategoryTree() {
  return request.get('/categories/tree')
}

/**
 * 新增分类
 * @param {Object} data - 分类数据
 * @param {Long|null} data.parentId - 父分类ID
 * @param {String} data.name - 分类名称
 * @param {Integer} data.sortOrder - 排序序号
 */
export function createCategory(data) {
  return request.post('/categories', data)
}

/**
 * 修改分类
 * @param {Long} id - 分类ID
 * @param {Object} data - 更新数据
 * @param {String} data.name - 分类名称
 * @param {Integer} data.sortOrder - 排序序号
 */
export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

/**
 * 删除分类
 * @param {Long} id - 分类ID
 */
export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
