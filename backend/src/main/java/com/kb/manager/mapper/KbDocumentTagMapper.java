package com.kb.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.manager.entity.KbDocumentTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档标签Mapper接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-12
 */
@Mapper
public interface KbDocumentTagMapper extends BaseMapper<KbDocumentTag> {
    // 可扩展自定义查询方法
}
