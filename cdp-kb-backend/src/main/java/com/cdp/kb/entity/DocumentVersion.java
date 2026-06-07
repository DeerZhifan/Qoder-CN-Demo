package com.cdp.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_version")
public class DocumentVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Integer version;
    
    private String content;
    
    private String changeLog;
    
    private LocalDateTime createdAt;
}
