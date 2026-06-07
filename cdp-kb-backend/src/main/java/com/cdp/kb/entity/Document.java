package com.cdp.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private Long categoryId;
    
    private String content;
    
    private Integer status;
    
    private Integer version;
    
    private Integer publishedVersion;
    
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime publishedAt;
}
