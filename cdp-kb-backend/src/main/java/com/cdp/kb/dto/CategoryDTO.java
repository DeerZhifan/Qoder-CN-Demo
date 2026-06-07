package com.cdp.kb.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CategoryDTO {
    private Long id;
    
    @NotBlank(message = "分类名称不能为空")
    private String name;
    
    private Long parentId;
    
    private Integer sortOrder;
}
