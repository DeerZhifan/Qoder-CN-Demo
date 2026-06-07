package com.cdp.kb.controller;

import com.cdp.kb.common.Result;
import com.cdp.kb.dto.CategoryDTO;
import com.cdp.kb.entity.Category;
import com.cdp.kb.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * 获取分类列表
     */
    @GetMapping
    public Result<List<Category>> listCategories() {
        List<Category> categories = categoryService.listCategories();
        return Result.success(categories);
    }
    
    /**
     * 创建分类
     */
    @PostMapping
    public Result<Category> createCategory(@Valid @RequestBody CategoryDTO dto) {
        try {
            Category category = categoryService.createCategory(dto);
            return Result.success(category);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        try {
            Category category = categoryService.updateCategory(id, dto);
            return Result.success(category);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
