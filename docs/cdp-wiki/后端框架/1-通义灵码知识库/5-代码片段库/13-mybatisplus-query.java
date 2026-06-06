package com.leatop.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leatop.example.dao.NewsItemDao;
import com.leatop.example.model.po.NewsItemPo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * MyBatis-Plus 常用查询写法示例
 *
 * 规范：
 * - 优先使用 LambdaQueryWrapper（类型安全，重构友好），避免硬编码字段名
 * - 条件判断：字段有值时才加条件（xxx(condition, column, val) 三参数形式）
 * - 排序、分页、LIMIT 等放在 wrapper 末尾
 */
@Service
public class MyBatisPlusQueryExample extends ServiceImpl<NewsItemDao, NewsItemPo> {

    /**
     * 示例一：带条件的分页查询（最常用）
     */
    public List<NewsItemPo> queryByCondition(String title, String source, Integer status) {
        LambdaQueryWrapper<NewsItemPo> wrapper = new LambdaQueryWrapper<>();

        // 条件：值不为空时才追加（避免全表扫描）
        wrapper.like(title != null && !title.isBlank(), NewsItemPo::getTitle, title);
        wrapper.like(source != null && !source.isBlank(), NewsItemPo::getSource, source);
        wrapper.eq(Objects.nonNull(status), NewsItemPo::getStatus, status);

        // 排序
        wrapper.orderByDesc(NewsItemPo::getCreateGmt);

        return getBaseMapper().selectList(wrapper);
    }

    /**
     * 示例二：查询单条记录（取第一条匹配，加 LIMIT 1 防止多条时报错）
     */
    public NewsItemPo getFirstBySource(String source) {
        return getOne(new LambdaQueryWrapper<NewsItemPo>()
                .eq(NewsItemPo::getSource, source)
                .orderByDesc(NewsItemPo::getCreateGmt)
                .last("LIMIT 1"));  // 数据库原生 LIMIT，用于只取一条
    }

    /**
     * 示例三：只更新指定字段（避免全字段 update 覆盖其他字段）
     */
    public void updateStatusById(String id, Integer status) {
        LambdaUpdateWrapper<NewsItemPo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NewsItemPo::getId, id)
               .set(NewsItemPo::getStatus, status);
        update(wrapper);
    }

    /**
     * 示例四：统计数量
     */
    public long countByStatus(Integer status) {
        return count(new LambdaQueryWrapper<NewsItemPo>()
                .eq(NewsItemPo::getStatus, status));
    }

    /**
     * 示例五：QueryWrapper 写法（当 LambdaQueryWrapper 不满足需求时使用）
     * 如：需要数据库函数、子查询等
     */
    public NewsItemPo getByIdUsingQueryWrapper(String id) {
        return getOne(new QueryWrapper<NewsItemPo>().lambda()
                .eq(NewsItemPo::getId, id)
                .last("LIMIT 1"));
    }
}
