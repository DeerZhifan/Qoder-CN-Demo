package com.cdp.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdp.kb.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VersionMapper extends BaseMapper<DocumentVersion> {
}
