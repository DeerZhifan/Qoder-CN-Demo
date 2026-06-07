package com.kb.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.manager.dto.DocumentQueryDTO;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.entity.KbDocumentVersion;

import java.util.List;

/**
 * 知识库文档服务接口
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
public interface KbDocumentService {
    
    /**
     * 分页查询文档列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    Page<KbDocument> pageDocuments(DocumentQueryDTO query);
    
    /**
     * 获取文档详情
     * 
     * @param id 文档ID
     * @return 文档实体
     */
    KbDocument getDocumentById(Long id);
    
    /**
     * 创建文档草稿
     * 
     * @param categoryId 分类ID
     * @param title 文档标题
     * @param content 文档内容
     * @return 文档ID
     */
    Long createDocument(Long categoryId, String title, String content);
    
    /**
     * 更新文档(仅草稿状态可更新)
     * 
     * @param id 文档ID
     * @param title 文档标题
     * @param content 文档内容
     */
    void updateDocument(Long id, String title, String content);
    
    /**
     * 发布文档(草稿->已发布,创建新版本)
     * 
     * @param id 文档ID
     */
    void publishDocument(Long id);
    
    /**
     * 下线文档(已发布->已下线)
     * 
     * @param id 文档ID
     */
    void offlineDocument(Long id);
    
    /**
     * 删除文档(软删除)
     * 
     * @param id 文档ID
     */
    void deleteDocument(Long id);
    
    /**
     * 获取文档版本列表
     * 
     * @param documentId 文档ID
     * @return 版本列表
     */
    List<KbDocumentVersion> getVersionList(Long documentId);
    
    /**
     * 版本回滚
     * 
     * @param documentId 文档ID
     * @param versionId 版本ID
     */
    void rollbackVersion(Long documentId, Long versionId);
}
