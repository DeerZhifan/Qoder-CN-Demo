package com.leatop.example.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PO（持久化对象）示例 — 与数据库表一一对应
 *
 * 规范说明：
 * - 类名以 PO 结尾，与表名对应
 * - @TableName 指定表名
 * - @TableId 指定主键，type=IdType.ASSIGN_UUID 使用 UUID 策略
 * - @TableField 指定列名（与属性名一致时可省略）
 * - 实现 Serializable，声明 serialVersionUID
 * - 使用 @Accessors(chain = true) 支持链式调用
 * - 不包含业务逻辑，仅做字段映射
 */
@Accessors(chain = true)
@Data
@TableName("news_item")
public class NewsItemPo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（UUID）
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 文章标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 来源
     */
    @TableField(value = "source")
    private String source;

    /**
     * 状态：0-未解析，1-解析成功，2-解析异常
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 租户ID（多租户场景必填）
     */
    @TableField(value = "tenant_id")
    private String tenantId;

    /**
     * 创建时间
     */
    @TableField(value = "create_gmt")
    private LocalDateTime createGmt;

    /**
     * 更新时间
     */
    @TableField(value = "update_gmt")
    private LocalDateTime updateGmt;
}
