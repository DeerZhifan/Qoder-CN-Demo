package com.cdp.kb.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class DocumentDTO {
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    
    @NotBlank(message = "正文不能为空")
    private String content;
    
    private Integer status;
    
    private String changeLog;
}
