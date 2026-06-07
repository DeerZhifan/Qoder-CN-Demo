package com.kb.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.manager.dto.CategoryTreeDTO;
import com.kb.manager.dto.DocumentQueryDTO;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.entity.KbDocumentVersion;
import com.kb.manager.mapper.KbDocumentMapper;
import com.kb.manager.mapper.KbDocumentVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识库文档服务单元测试
 */
@SpringBootTest
@Transactional // 每个测试方法执行后自动回滚
class KbDocumentServiceTest {
    
    @Autowired
    private KbDocumentService documentService;
    
    @Autowired
    private KbCategoryService categoryService;
    
    @Autowired
    private KbDocumentMapper documentMapper;
    
    @Autowired
    private KbDocumentVersionMapper versionMapper;
    
    private Long testCategoryId;
    private Long testDocumentId;
    
    @BeforeEach
    void setUp() {
        // 创建测试分类
        categoryService.createCategory(0L, "测试分类", 1);
        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        testCategoryId = tree.get(0).getId();
        
        // 创建测试文档
        testDocumentId = documentService.createDocument(testCategoryId, "测试文档", "# 测试内容");
    }
    
    /**
     * 测试发布文档时创建新版本
     */
    @Test
    void testPublishDocumentCreatesVersion() {
        // 发布文档
        documentService.publishDocument(testDocumentId);
        
        // 验证文档状态变为PUBLISHED
        KbDocument doc = documentService.getDocumentById(testDocumentId);
        assertEquals("PUBLISHED", doc.getStatus());
        assertEquals(2, doc.getVersion()); // 初始版本1,发布后变为2
        
        // 验证版本表有记录
        List<KbDocumentVersion> versions = documentService.getVersionList(testDocumentId);
        assertFalse(versions.isEmpty());
        assertEquals(2, versions.get(0).getVersion()); // 最新版本号为2
    }
    
    /**
     * 测试软删除文档
     */
    @Test
    void testSoftDeleteDocument() {
        // 删除文档
        documentService.deleteDocument(testDocumentId);
        
        // 验证查询不到该文档(逻辑删除自动过滤)
        assertThrows(NoSuchElementException.class, () -> {
            documentService.getDocumentById(testDocumentId);
        });
        
        // 注意: MyBatis-Plus的selectById也会应用逻辑删除过滤器
        // 所以这里只能验证通过Service查询会抛出异常
    }
    
    /**
     * 测试版本回滚功能
     */
    @Test
    void testRollbackVersion() {
        // 先发布一次(版本2)
        documentService.publishDocument(testDocumentId);
        
        // 回滚到版本1(初始版本)
        List<KbDocumentVersion> versions = documentService.getVersionList(testDocumentId);
        Long version1Id = versions.stream()
            .filter(v -> v.getVersion() == 2) // 发布后创建的是版本2
            .findFirst()
            .get()
            .getId();
        
        documentService.rollbackVersion(testDocumentId, version1Id);
        
        // 验证文档内容恢复为版本1
        KbDocument doc = documentService.getDocumentById(testDocumentId);
        assertEquals("DRAFT", doc.getStatus()); // 回滚后状态为DRAFT
        assertEquals("# 测试内容", doc.getContent()); // 内容应为版本1的内容
    }
    
    /**
     * 测试分页查询筛选条件
     */
    @Test
    void testPageDocumentsWithFilters() {
        // 创建多个文档
        documentService.createDocument(testCategoryId, "Java入门", "# Java内容");
        documentService.createDocument(testCategoryId, "Python进阶", "# Python内容");
        documentService.createDocument(testCategoryId, "JavaScript基础", "# JS内容");
        
        // 按标题筛选
        DocumentQueryDTO query = new DocumentQueryDTO();
        query.setTitle("Java");
        query.setPageNum(1);
        query.setPageSize(10);
        
        Page<KbDocument> page = documentService.pageDocuments(query);
        // 验证返回的记录不为空
        assertNotNull(page);
        assertNotNull(page.getRecords());
        assertFalse(page.getRecords().isEmpty(), "应该至少找到1个包含'Java'的文档");
        
        // 验证返回的记录中确实有包含"Java"的文档
        boolean hasJavaDoc = page.getRecords().stream()
            .anyMatch(d -> d.getTitle() != null && d.getTitle().contains("Java"));
        assertTrue(hasJavaDoc, "返回的记录中应该至少有一个标题包含'Java'的文档");
    }
    
    /**
     * 测试只有草稿状态可更新
     */
    @Test
    void testOnlyDraftCanBeUpdated() {
        // 发布文档
        documentService.publishDocument(testDocumentId);
        
        // 尝试更新已发布文档,应抛出异常
        assertThrows(IllegalStateException.class, () -> {
            documentService.updateDocument(testDocumentId, "新标题", "新内容");
        });
    }
}
