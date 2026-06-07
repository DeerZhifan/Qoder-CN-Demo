package com.kb.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.manager.entity.KbCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库分类Mapper接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Mapper
public interface KbCategoryMapper extends BaseMapper<KbCategory> {
    // 可扩展自定义查询方法
}
