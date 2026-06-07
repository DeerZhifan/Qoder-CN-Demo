package com.kb.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("kb_document")
public class KbDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 文档标题
     */
    @TableField("title")
    private String title;

    /**
     * 文档内容（Markdown格式）
     */
    @TableField("content")
    private String content;

    /**
     * 文档状态：DRAFT-草稿，PUBLISHED-已发布，OFFLINE-已下线
     */
    @TableField("status")
    private String status;

    /**
     * 当前版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
