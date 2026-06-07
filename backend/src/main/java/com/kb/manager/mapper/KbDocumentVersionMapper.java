package com.kb.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.manager.entity.KbDocumentVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档版本Mapper接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Mapper
public interface KbDocumentVersionMapper extends BaseMapper<KbDocumentVersion> {
    // 可扩展自定义查询方法
}
