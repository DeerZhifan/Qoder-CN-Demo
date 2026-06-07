package com.kb.manager.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 分类创建请求DTO
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
public class CategoryCreateRequest {
    
    /**
     * 父分类ID,null表示根节点
     */
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;
    
    /**
     * 排序序号,默认0
     */
    private Integer sortOrder = 0;
}
