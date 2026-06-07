package com.kb.manager.dto;

import lombok.Data;

import java.util.List;

/**
 * 分类树形结构DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
public class CategoryTreeDTO {
    
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 父分类ID
     */
    private Long parentId;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 子分类列表
     */
    private List<CategoryTreeDTO> children;
}
