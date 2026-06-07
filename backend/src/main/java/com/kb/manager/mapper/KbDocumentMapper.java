package com.kb.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.manager.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档Mapper接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {
    // 可扩展自定义查询方法
}
