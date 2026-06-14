package com.kb.manager.service;

import com.kb.manager.dto.CategoryDocumentCountDTO;
import com.kb.manager.dto.CategoryTreeDTO;

import java.util.List;

/**
 * 知识库分类服务接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
public interface KbCategoryService {
    
    /**
     * 获取分类树形结构
     * 
     * @return 分类树列表
     */
    List<CategoryTreeDTO> getCategoryTree();
    
    /**
     * 创建分类
     * 
     * @param parentId 父分类ID,null表示根节点
     * @param name 分类名称
     * @param sortOrder 排序序号
     * @return 创建的分类ID
     */
    Long createCategory(Long parentId, String name, Integer sortOrder);
    
    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param name 分类名称
     * @param sortOrder 排序序号
     */
    void updateCategory(Long id, String name, Integer sortOrder);
    
    /**
     * 删除分类(软删除)
     * 
     * @param id 分类ID
     */
    void deleteCategory(Long id);
    
    /**
     * 按分类统计文档数量
     * 
     * @return 分类文档数量统计列表
     */
    List<CategoryDocumentCountDTO> getCategoryDocumentCount();
}
