package com.kb.manager.service;

import com.kb.manager.dto.CategoryTreeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识库分类服务单元测试
 */
@SpringBootTest
@Transactional
class KbCategoryServiceTest {
    
    @Autowired
    private KbCategoryService categoryService;
    
    @Autowired
    private KbDocumentService documentService;
    
    /**
     * 测试分类树构建(避免N+1)
     */
    @Test
    void testGetCategoryTree() {
        // 创建多级分类
        categoryService.createCategory(0L, "技术文档", 1);
        categoryService.createCategory(0L, "产品文档", 2);
        
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        
        // 获取技术文档分类ID
        Long techId = tree.stream()
            .filter(c -> "技术文档".equals(c.getName()))
            .findFirst()
            .get()
            .getId();
        
        // 创建子分类
        categoryService.createCategory(techId, "后端开发", 1);
        categoryService.createCategory(techId, "前端开发", 2);
        
        // 重新获取树
        tree = categoryService.getCategoryTree();
        CategoryTreeDTO techNode = tree.stream()
            .filter(c -> "技术文档".equals(c.getName()))
            .findFirst()
            .get();
        
        assertEquals(2, techNode.getChildren().size());
        // 验证子分类存在（不依赖具体顺序）
        List<String> childNames = techNode.getChildren().stream()
            .map(CategoryTreeDTO::getName)
            .toList();
        assertTrue(childNames.contains("后端开发"));
        assertTrue(childNames.contains("前端开发"));
    }
    
    /**
     * 测试删除分类时的关联检查
     */
    @Test
    void testDeleteCategoryWithChildren() {
        // 创建父分类和子分类
        categoryService.createCategory(0L, "父分类", 1);
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        Long parentId = tree.get(0).getId();
        
        categoryService.createCategory(parentId, "子分类", 1);
        
        // 尝试删除有子分类的父分类,应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.deleteCategory(parentId);
        });
    }
    
    /**
     * 测试删除分类时的文档关联检查
     */
    @Test
    void testDeleteCategoryWithDocuments() {
        // 创建分类
        categoryService.createCategory(0L, "有文档的分类", 1);
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        Long categoryId = tree.get(0).getId();
        
        // 在该分类下创建文档
        documentService.createDocument(categoryId, "测试文档", "# 内容");
        
        // 尝试删除有关联文档的分类,应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.deleteCategory(categoryId);
        });
    }
    
    /**
     * 测试成功删除空分类
     */
    @Test
    void testDeleteEmptyCategory() {
        // 创建空分类
        categoryService.createCategory(0L, "空分类", 1);
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        Long categoryId = tree.get(0).getId();
        
        // 删除空分类,应成功
        categoryService.deleteCategory(categoryId);
        
        // 验证分类已从树中消失
        tree = categoryService.getCategoryTree();
        assertTrue(tree.stream().noneMatch(c -> "空分类".equals(c.getName())));
    }
}
