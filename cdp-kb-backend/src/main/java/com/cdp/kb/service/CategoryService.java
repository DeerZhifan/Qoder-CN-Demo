package com.cdp.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdp.kb.dto.CategoryDTO;
import com.cdp.kb.entity.Category;
import com.cdp.kb.mapper.CategoryMapper;
import com.cdp.kb.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryMapper categoryMapper;
    private final DocumentMapper documentMapper;
    
    /**
     * 获取所有分类（树形结构）
     */
    public List<Category> listCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder, Category::getId);
        return categoryMapper.selectList(wrapper);
    }
    
    /**
     * 创建分类
     */
    @Transactional
    public Category createCategory(CategoryDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }
        
        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        
        categoryMapper.insert(category);
        return category;
    }
    
    /**
     * 更新分类
     */
    @Transactional
    public Category updateCategory(Long id, CategoryDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }
        
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        
        categoryMapper.updateById(category);
        return category;
    }
    
    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否有文档关联
        LambdaQueryWrapper<com.cdp.kb.entity.Document> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(com.cdp.kb.entity.Document::getCategoryId, id);
        long count = documentMapper.selectCount(docWrapper);
        if (count > 0) {
            throw new RuntimeException("该分类下还有文档，无法删除");
        }
        
        categoryMapper.deleteById(id);
    }
}
