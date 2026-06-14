package com.kb.manager.dto;

import lombok.Data;

/**
 * 分类文档数量统计DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-11
 */
@Data
public class CategoryDocumentCountDTO {
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 文档数量
     */
    private Long documentCount;
}
