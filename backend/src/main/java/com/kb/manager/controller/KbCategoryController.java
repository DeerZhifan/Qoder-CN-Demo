package com.kb.manager.controller;

import com.kb.manager.common.Result;
import com.kb.manager.dto.CategoryCreateRequest;
import com.kb.manager.dto.CategoryDocumentCountDTO;
import com.kb.manager.dto.CategoryTreeDTO;
import com.kb.manager.dto.CategoryUpdateRequest;
import com.kb.manager.service.KbCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 知识库分类控制器
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/categories")
@Validated
public class KbCategoryController {
    
    @Autowired
    private KbCategoryService categoryService;
    
    /**
     * 获取分类树形结构
     * 
     * @return 分类树列表
     */
    @GetMapping("/tree")
    public Result<List<CategoryTreeDTO>> getCategoryTree() {
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }
    
    /**
     * 新增分类
     * 
     * @param request 分类创建请求
     * @return 创建的分类ID
     */
    @PostMapping
    public Result<Long> createCategory(@RequestBody @Valid CategoryCreateRequest request) {
        Long categoryId = categoryService.createCategory(request.getParentId(), request.getName(), request.getSortOrder());
        return Result.success(categoryId);
    }
    
    /**
     * 修改分类
     * 
     * @param id 分类ID
     * @param request 分类更新请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryUpdateRequest request) {
        categoryService.updateCategory(id, request.getName(), request.getSortOrder());
        return Result.success(null);
    }
    
    /**
     * 删除分类(软删除)
     * 
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success(null);
    }
    
    /**
     * 按分类统计文档数量
     * 
     * @return 分类文档数量统计列表
     */
    @GetMapping("/document-count")
    public Result<List<CategoryDocumentCountDTO>> getCategoryDocumentCount() {
        List<CategoryDocumentCountDTO> countList = categoryService.getCategoryDocumentCount();
        return Result.success(countList);
    }
}
