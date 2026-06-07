package com.kb.manager.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类更新请求DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
public class CategoryUpdateRequest {
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
}
