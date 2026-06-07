package com.kb.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.manager.dto.DocumentQueryDTO;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.entity.KbDocumentVersion;
import com.kb.manager.mapper.KbDocumentMapper;
import com.kb.manager.mapper.KbDocumentVersionMapper;
import com.kb.manager.service.KbDocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 知识库文档服务实现类
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Service
public class KbDocumentServiceImpl implements KbDocumentService {
    
    private final KbDocumentMapper documentMapper;
    private final KbDocumentVersionMapper versionMapper;
    
    public KbDocumentServiceImpl(KbDocumentMapper documentMapper, 
                                 KbDocumentVersionMapper versionMapper) {
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
    }
    
    @Override
    public Page<KbDocument> pageDocuments(DocumentQueryDTO query) {
        // 构建分页对象
        Page<KbDocument> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        // 构建查询条件
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getCategoryId() != null) {
            wrapper.eq(KbDocument::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(KbDocument::getStatus, query.getStatus());
        }
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            wrapper.like(KbDocument::getTitle, query.getTitle());
        }
        
        // 只查询未删除的文档,按创建时间倒序
        wrapper.eq(KbDocument::getDeleted, 0)
               .orderByDesc(KbDocument::getCreateTime);
        
        return documentMapper.selectPage(page, wrapper);
    }
    
    @Override
    public KbDocument getDocumentById(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + id);
        }
        return document;
    }
    
    @Override
    @Transactional
    public Long createDocument(Long categoryId, String title, String content) {
        // 参数校验
        if (categoryId == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("标题长度不能超过200字符");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        
        // 创建文档草稿
        KbDocument document = new KbDocument();
        document.setCategoryId(categoryId);
        document.setTitle(title.trim());
        document.setContent(content);
        document.setStatus("DRAFT");
        document.setVersion(1);
        document.setCreateBy(getCurrentUser());
        document.setUpdateBy(getCurrentUser());
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setDeleted(0);
        
        documentMapper.insert(document);
        
        return document.getId();
    }
    
    @Override
    @Transactional
    public void updateDocument(Long id, String title, String content) {
        // 查询文档
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + id);
        }
        
        // 只有草稿状态可以更新
        if (!"DRAFT".equals(document.getStatus())) {
            throw new IllegalStateException("只有草稿状态的文档可以更新");
        }
        
        // 更新标题
        if (title != null && !title.trim().isEmpty()) {
            if (title.length() > 200) {
                throw new IllegalArgumentException("标题长度不能超过200字符");
            }
            document.setTitle(title.trim());
        }
        
        // 更新内容
        if (content != null && !content.trim().isEmpty()) {
            document.setContent(content);
        }
        
        document.setUpdateBy(getCurrentUser());
        document.setUpdateTime(LocalDateTime.now());
        
        documentMapper.updateById(document);
    }
    
    @Override
    @Transactional
    public void publishDocument(Long id) {
        // 查询文档
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + id);
        }
        
        // 只有草稿状态可以发布
        if (!"DRAFT".equals(document.getStatus())) {
            throw new IllegalStateException("只有草稿状态的文档可以发布");
        }
        
        // 创建新版本记录
        int newVersion = document.getVersion() + 1;
        KbDocumentVersion version = new KbDocumentVersion();
        version.setDocumentId(id);
        version.setVersion(newVersion);
        version.setTitle(document.getTitle());
        version.setContent(document.getContent());
        version.setCreateBy(getCurrentUser());
        version.setCreateTime(LocalDateTime.now());
        versionMapper.insert(version);
        
        // 更新文档状态为已发布
        document.setStatus("PUBLISHED");
        document.setVersion(newVersion);
        document.setPublishTime(LocalDateTime.now());
        document.setUpdateBy(getCurrentUser());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
    }
    
    @Override
    @Transactional
    public void offlineDocument(Long id) {
        // 查询文档
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + id);
        }
        
        // 只有已发布状态可以下线
        if (!"PUBLISHED".equals(document.getStatus())) {
            throw new IllegalStateException("只有已发布的文档可以下线");
        }
        
        // 更新状态为已下线
        document.setStatus("OFFLINE");
        document.setUpdateBy(getCurrentUser());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
    }
    
    @Override
    @Transactional
    public void deleteDocument(Long id) {
        // 查询文档
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + id);
        }
        
        // 软删除文档(MyBatis-Plus的@TableLogic会自动处理)
        documentMapper.deleteById(id);
    }
    
    @Override
    public List<KbDocumentVersion> getVersionList(Long documentId) {
        // 验证文档是否存在
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + documentId);
        }
        
        // 查询该文档的所有版本,按版本号倒序
        LambdaQueryWrapper<KbDocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocumentVersion::getDocumentId, documentId)
               .orderByDesc(KbDocumentVersion::getVersion);
        
        return versionMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public void rollbackVersion(Long documentId, Long versionId) {
        // 查询版本
        KbDocumentVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getDocumentId().equals(documentId)) {
            throw new NoSuchElementException("版本不存在: " + versionId);
        }
        
        // 查询文档
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new NoSuchElementException("文档不存在: " + documentId);
        }
        
        // 将版本文档内容复制回当前文档,状态改为DRAFT
        document.setTitle(version.getTitle());
        document.setContent(version.getContent());
        document.setStatus("DRAFT");
        document.setUpdateBy(getCurrentUser());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
    }
    
    /**
     * 获取当前用户(实际项目中应从SecurityContext或Session中获取)
     * TODO: 集成Spring Security后替换为真实用户信息
     */
    private String getCurrentUser() {
        // 临时返回固定值,实际应该从认证上下文获取
        return "system";
    }
}
