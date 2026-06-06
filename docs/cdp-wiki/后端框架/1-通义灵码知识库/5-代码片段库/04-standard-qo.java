package com.leatop.example.model.qo;

import com.leatop.cdp.data.qo.PageQo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * QO（查询参数对象）示例 — 专用于列表查询条件封装
 *
 * 规范说明：
 * - 类名以 QO 结尾（分页查询用 PageQo 命名）
 * - 继承 PageQo 获得 page、size 分页参数
 * - 只放查询条件字段，不放排序、格式化等逻辑
 * - 字段为 null 时表示该条件不生效（在 ServiceImpl 中用 Wrapper 判断）
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class NewsItemPageQo extends PageQo {

    /**
     * 文章标题（模糊查询）
     */
    private String title;

    /**
     * 来源（模糊查询）
     */
    private String source;

    /**
     * 状态（精确查询）
     */
    private Integer status;
}
