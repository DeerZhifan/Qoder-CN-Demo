package com.kb.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.manager.common.Result;
import com.kb.manager.dto.DocumentCreateRequest;
import com.kb.manager.dto.DocumentQueryDTO;
import com.kb.manager.dto.DocumentUpdateRequest;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.entity.KbDocumentVersion;
import com.kb.manager.service.KbDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 知识库文档控制器
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/documents")
@Validated
public class KbDocumentController {
    
    @Autowired
    private KbDocumentService documentService;
    
    /**
     * 分页查询文档列表
     * 
     * @param categoryId 分类ID(可选)
     * @param status 文档状态(可选)
     * @param title 文档标题(可选,支持模糊查询)
     * @param pageNum 页码,默认1
     * @param pageSize 每页数量,默认10
     * @return 分页结果
     */
    @GetMapping
    public Result<Page<KbDocument>> pageDocuments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        DocumentQueryDTO query = new DocumentQueryDTO();
        query.setCategoryId(categoryId);
        query.setStatus(status);
        query.setTitle(title);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        Page<KbDocument> page = documentService.pageDocuments(query);
        return Result.success(page);
    }
    
    /**
     * 获取文档详情
     * 
     * @param id 文档ID
     * @return 文档详情
     */
    @GetMapping("/{id}")
    public Result<KbDocument> getDocument(@PathVariable Long id) {
        KbDocument doc = documentService.getDocumentById(id);
        return Result.success(doc);
    }
    
    /**
     * 创建文档草稿
     * 
     * @param request 文档创建请求
     * @return 文档ID
     */
    @PostMapping
    public Result<Long> createDocument(@RequestBody @Valid DocumentCreateRequest request) {
        Long id = documentService.createDocument(request.getCategoryId(), request.getTitle(), request.getContent());
        return Result.success(id);
    }
    
    /**
     * 更新文档(仅草稿状态)
     * 
     * @param id 文档ID
     * @param request 文档更新请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> updateDocument(@PathVariable Long id, @RequestBody @Valid DocumentUpdateRequest request) {
        documentService.updateDocument(id, request.getTitle(), request.getContent());
        return Result.success(null);
    }
    
    /**
     * 发布文档
     * 
     * @param id 文档ID
     * @return 操作结果
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publishDocument(@PathVariable Long id) {
        documentService.publishDocument(id);
        return Result.success(null);
    }
    
    /**
     * 下线文档
     * 
     * @param id 文档ID
     * @return 操作结果
     */
    @PostMapping("/{id}/offline")
    public Result<Void> offlineDocument(@PathVariable Long id) {
        documentService.offlineDocument(id);
        return Result.success(null);
    }
    
    /**
     * 删除文档(软删除)
     * 
     * @param id 文档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success(null);
    }
    
    /**
     * 获取版本列表
     * 
     * @param id 文档ID
     * @return 版本列表
     */
    @GetMapping("/{id}/versions")
    public Result<List<KbDocumentVersion>> getVersionList(@PathVariable Long id) {
        List<KbDocumentVersion> versions = documentService.getVersionList(id);
        return Result.success(versions);
    }
    
    /**
     * 版本回滚
     * 
     * @param id 文档ID
     * @param versionId 版本ID
     * @return 操作结果
     */
    @PostMapping("/{id}/rollback/{versionId}")
    public Result<Void> rollbackVersion(@PathVariable Long id, @PathVariable Long versionId) {
        documentService.rollbackVersion(id, versionId);
        return Result.success(null);
    }
}
