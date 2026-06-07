package com.kb.manager.dto;

import lombok.Data;

/**
 * 文档查询条件DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
public class DocumentQueryDTO {
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 文档状态: DRAFT/PUBLISHED/OFFLINE
     */
    private String status;
    
    /**
     * 文档标题(支持模糊查询)
     */
    private String title;
    
    /**
     * 页码,默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量,默认10
     */
    private Integer pageSize = 10;
}
