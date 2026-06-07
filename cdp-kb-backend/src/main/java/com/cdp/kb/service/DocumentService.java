package com.cdp.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdp.kb.common.PageResult;
import com.cdp.kb.dto.DocumentDTO;
import com.cdp.kb.entity.Document;
import com.cdp.kb.entity.DocumentVersion;
import com.cdp.kb.mapper.DocumentMapper;
import com.cdp.kb.mapper.VersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentMapper documentMapper;
    private final VersionMapper versionMapper;
    
    /**
     * 分页查询文档列表
     */
    public PageResult<Document> listDocuments(Integer current, Integer size, Long categoryId, Integer status) {
        Page<Document> page = new Page<>(current, size);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null) {
            wrapper.eq(Document::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Document::getStatus, status);
        }
        
        wrapper.orderByDesc(Document::getUpdatedAt);
        Page<Document> result = documentMapper.selectPage(page, wrapper);
        
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    /**
     * 获取文档详情
     */
    public Document getDocument(Long id) {
        return documentMapper.selectById(id);
    }
    
    /**
     * 创建文档
     */
    @Transactional
    public Document createDocument(DocumentDTO dto) {
        validateDocument(dto);
        
        Document document = new Document();
        document.setTitle(dto.getTitle());
        document.setCategoryId(dto.getCategoryId());
        document.setContent(dto.getContent());
        document.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        document.setVersion(1);
        
        documentMapper.insert(document);
        return document;
    }
    
    /**
     * 更新文档
     */
    @Transactional
    public Document updateDocument(Long id, DocumentDTO dto) {
        validateDocument(dto);
        
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        document.setTitle(dto.getTitle());
        document.setCategoryId(dto.getCategoryId());
        document.setContent(dto.getContent());
        if (dto.getStatus() != null) {
            document.setStatus(dto.getStatus());
        }
        
        // 如果内容有变化，创建新版本
        if (!document.getContent().equals(dto.getContent())) {
            document.setVersion(document.getVersion() + 1);
            saveVersion(document.getId(), document.getVersion(), dto.getContent(), dto.getChangeLog());
        }
        
        documentMapper.updateById(document);
        return document;
    }
    
    /**
     * 软删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 已发布的文档需要先下线
        if (document.getStatus() == 1) {
            throw new RuntimeException("已发布的文档需要先下线才能删除");
        }
        
        documentMapper.deleteById(id); // MyBatis-Plus 会自动处理逻辑删除
    }
    
    /**
     * 恢复已删除文档
     */
    @Transactional
    public void restoreDocument(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 手动重置 deleted 字段（因为 @TableLogic 会过滤）
        document.setDeleted(0);
        documentMapper.updateById(document);
    }
    
    /**
     * 发布文档
     */
    @Transactional
    public void publishDocument(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 校验必填项
        if (!StringUtils.hasText(document.getTitle())) {
            throw new RuntimeException("标题不能为空，无法发布");
        }
        if (document.getCategoryId() == null) {
            throw new RuntimeException("分类不能为空，无法发布");
        }
        if (!StringUtils.hasText(document.getContent())) {
            throw new RuntimeException("正文不能为空，无法发布");
        }
        
        // 只有草稿或已下线状态可以发布
        if (document.getStatus() != 0 && document.getStatus() != 2) {
            throw new RuntimeException("当前状态不允许发布");
        }
        
        // 创建新版本
        int newVersion = document.getVersion() + 1;
        saveVersion(document.getId(), newVersion, document.getContent(), "发布版本");
        
        // 更新文档状态
        document.setVersion(newVersion);
        document.setPublishedVersion(newVersion);
        document.setStatus(1);
        document.setPublishedAt(LocalDateTime.now());
        
        documentMapper.updateById(document);
    }
    
    /**
     * 下线文档
     */
    @Transactional
    public void offlineDocument(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        if (document.getStatus() != 1) {
            throw new RuntimeException("只有已发布的文档可以下线");
        }
        
        document.setStatus(2);
        documentMapper.updateById(document);
    }
    
    /**
     * 获取已发布的文档列表（供前台使用）
     */
    public List<Document> listPublished() {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getStatus, 1)
               .orderByDesc(Document::getPublishedAt);
        return documentMapper.selectList(wrapper);
    }
    
    /**
     * 保存版本记录
     */
    private void saveVersion(Long documentId, Integer version, String content, String changeLog) {
        DocumentVersion versionEntity = new DocumentVersion();
        versionEntity.setDocumentId(documentId);
        versionEntity.setVersion(version);
        versionEntity.setContent(content);
        versionEntity.setChangeLog(changeLog);
        versionMapper.insert(versionEntity);
    }
    
    /**
     * 校验文档必填项
     */
    private void validateDocument(DocumentDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new RuntimeException("标题不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new RuntimeException("分类不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("正文不能为空");
        }
    }
}
