package com.cdp.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdp.kb.entity.DocumentVersion;
import com.cdp.kb.mapper.VersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VersionService {
    
    private final VersionMapper versionMapper;
    
    /**
     * 获取文档的版本列表
     */
    public List<DocumentVersion> listVersions(Long documentId) {
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId)
               .orderByDesc(DocumentVersion::getVersion);
        return versionMapper.selectList(wrapper);
    }
    
    /**
     * 获取指定版本的内容
     */
    public DocumentVersion getVersion(Long documentId, Integer version) {
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId)
               .eq(DocumentVersion::getVersion, version);
        return versionMapper.selectOne(wrapper);
    }
}
