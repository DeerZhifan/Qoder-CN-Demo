package com.leatop.example.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO（数据传输对象）示例 — 用于 Controller 入参/出参、Service 层数据传递
 *
 * 规范说明：
 * - 类名以 DTO 结尾
 * - 不含 @TableName、@TableField 等数据库注解
 * - 不继承任何框架基类
 * - 实现 Serializable，声明 serialVersionUID
 * - 字段与 PO 保持语义一致，可裁剪（如不对外暴露 tenantId）
 * - 如需参数校验，在字段上加 javax.validation 注解
 */
@Accessors(chain = true)
@Data
public class NewsItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 来源
     */
    private String source;

    /**
     * 状态：0-未解析，1-解析成功，2-解析异常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createGmt;

    /**
     * 更新时间
     */
    private LocalDateTime updateGmt;
}
