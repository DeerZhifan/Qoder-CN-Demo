package com.cdp.kb.controller;

import com.cdp.kb.common.PageResult;
import com.cdp.kb.common.Result;
import com.cdp.kb.dto.DocumentDTO;
import com.cdp.kb.entity.Document;
import com.cdp.kb.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 分页查询文档列表
     */
    @GetMapping
    public Result<PageResult<Document>> listDocuments(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        
        PageResult<Document> result = documentService.listDocuments(current, size, categoryId, status);
        return Result.success(result);
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public Result<Document> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocument(id);
        return Result.success(document);
    }
    
    /**
     * 创建文档
     */
    @PostMapping
    public Result<Document> createDocument(@Valid @RequestBody DocumentDTO dto) {
        try {
            Document document = documentService.createDocument(dto);
            return Result.success(document);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新文档
     */
    @PutMapping("/{id}")
    public Result<Document> updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentDTO dto) {
        try {
            Document document = documentService.updateDocument(id, dto);
            return Result.success(document);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 软删除文档
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 恢复已删除文档
     */
    @PostMapping("/{id}/restore")
    public Result<Void> restoreDocument(@PathVariable Long id) {
        try {
            documentService.restoreDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 发布文档
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publishDocument(@PathVariable Long id) {
        try {
            documentService.publishDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 下线文档
     */
    @PostMapping("/{id}/offline")
    public Result<Void> offlineDocument(@PathVariable Long id) {
        try {
            documentService.offlineDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取已发布的文档列表（供前台使用）
     */
    @GetMapping("/published")
    public Result<List<Document>> listPublished() {
        List<Document> documents = documentService.listPublished();
        return Result.success(documents);
    }
}
