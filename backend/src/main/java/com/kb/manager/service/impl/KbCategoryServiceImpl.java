package com.kb.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.manager.dto.CategoryTreeDTO;
import com.kb.manager.entity.KbCategory;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.mapper.KbCategoryMapper;
import com.kb.manager.mapper.KbDocumentMapper;
import com.kb.manager.service.KbCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 知识库分类服务实现类
 *
 * @author Knowledge Base Manager
 * @since 2026-06-07
 */
@Service
public class KbCategoryServiceImpl implements KbCategoryService {
    
    private final KbCategoryMapper categoryMapper;
    private final KbDocumentMapper documentMapper;
    
    public KbCategoryServiceImpl(KbCategoryMapper categoryMapper, KbDocumentMapper documentMapper) {
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }
    
    @Override
    public List<CategoryTreeDTO> getCategoryTree() {
        // 一次性加载所有未删除的分类,避免N+1查询
        List<KbCategory> allCategories = categoryMapper.selectList(
            new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getDeleted, 0)
                .orderByAsc(KbCategory::getSortOrder)
        );
        
        // 内存中构建树形结构
        Map<Long, CategoryTreeDTO> nodeMap = new HashMap<>();
        List<CategoryTreeDTO> rootNodes = new ArrayList<>();
        
        // 第一步: 将所有分类转换为DTO并放入Map
        for (KbCategory cat : allCategories) {
            CategoryTreeDTO node = convertToDTO(cat);
            node.setChildren(new ArrayList<>());
            nodeMap.put(cat.getId(), node);
        }
        
        // 第二步: 构建父子关系
        for (CategoryTreeDTO node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                // 根节点
                rootNodes.add(node);
            } else {
                // 子节点,添加到父节点的children中
                CategoryTreeDTO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        
        return rootNodes;
    }
    
    @Override
    @Transactional
    public Long createCategory(Long parentId, String name, Integer sortOrder) {
        // 参数校验
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("分类名称长度不能超过100字符");
        }
        if (sortOrder == null || sortOrder < 1) {
            throw new IllegalArgumentException("排序序号必须大于0");
        }
        
        // 如果指定了父分类,验证父分类是否存在
        if (parentId != null && parentId != 0) {
            KbCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("父分类不存在: " + parentId);
            }
        }
        
        // 创建分类
        KbCategory category = new KbCategory();
        category.setParentId(parentId != null ? parentId : 0L);
        category.setName(name.trim());
        category.setSortOrder(sortOrder);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(0);
        
        categoryMapper.insert(category);
        
        // 返回创建的分类ID
        return category.getId();
    }
    
    @Override
    @Transactional
    public void updateCategory(Long id, String name, Integer sortOrder) {
        // 查询分类是否存在
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在: " + id);
        }
        
        // 更新名称
        if (name != null && !name.trim().isEmpty()) {
            if (name.length() > 100) {
                throw new IllegalArgumentException("分类名称长度不能超过100字符");
            }
            category.setName(name.trim());
        }
        
        // 更新排序
        if (sortOrder != null) {
            if (sortOrder < 1) {
                throw new IllegalArgumentException("排序序号必须大于0");
            }
            category.setSortOrder(sortOrder);
        }
        
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }
    
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        // 查询分类是否存在
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在: " + id);
        }
        
        // 检查是否有子分类
        long childCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getParentId, id)
                .eq(KbCategory::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new IllegalArgumentException("该分类下存在子分类,无法删除");
        }
        
        // 检查是否有关联文档
        long docCount = documentMapper.selectCount(
            new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getCategoryId, id)
                .eq(KbDocument::getDeleted, 0)
        );
        if (docCount > 0) {
            throw new IllegalArgumentException("该分类下存在文档,无法删除");
        }
        
        // 软删除分类(MyBatis-Plus的@TableLogic会自动处理)
        categoryMapper.deleteById(id);
    }
    
    /**
     * 将实体转换为DTO
     */
    private CategoryTreeDTO convertToDTO(KbCategory category) {
        CategoryTreeDTO dto = new CategoryTreeDTO();
        BeanUtils.copyProperties(category, dto);
        return dto;
    }
}
