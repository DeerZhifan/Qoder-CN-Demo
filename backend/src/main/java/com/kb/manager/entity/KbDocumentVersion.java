package com.kb.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档版本实体
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("kb_document_version")
public class KbDocumentVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 版本文档标题
     */
    @TableField("title")
    private String title;

    /**
     * 版本文档内容（Markdown格式）
     */
    @TableField("content")
    private String content;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;
}
