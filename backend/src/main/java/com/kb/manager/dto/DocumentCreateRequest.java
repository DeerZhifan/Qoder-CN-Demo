package com.kb.manager.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文档创建请求DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
public class DocumentCreateRequest {
    
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    /**
     * 文档标题
     */
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题不能超过200个字符")
    private String title;
    
    /**
     * 文档内容(Markdown格式)
     */
    private String content;
}
