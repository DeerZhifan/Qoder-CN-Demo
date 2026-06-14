package com.kb.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.manager.dto.DocumentQueryDTO;
import com.kb.manager.entity.KbDocument;
import com.kb.manager.entity.KbDocumentVersion;
import com.kb.manager.mapper.KbDocumentMapper;
import com.kb.manager.mapper.KbDocumentVersionMapper;
import com.kb.manager.service.impl.KbDocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 知识库文档服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class KbDocumentServiceTest {
    
    @Mock
    private KbDocumentMapper documentMapper;
    
    @Mock
    private KbDocumentVersionMapper versionMapper;
    
    @InjectMocks
    private KbDocumentServiceImpl documentService;
    
    private Long testCategoryId;
    private Long testDocumentId;
    private KbDocument draftDocument;
    
    @BeforeEach
    void setUp() {
        testCategoryId = 1L;
        testDocumentId = 100L;
        
        // 创建草稿文档
        draftDocument = new KbDocument();
        draftDocument.setId(testDocumentId);
        draftDocument.setCategoryId(testCategoryId);
        draftDocument.setTitle("测试文档");
        draftDocument.setContent("# 测试内容");
        draftDocument.setStatus("DRAFT");
        draftDocument.setVersion(1);
        draftDocument.setPublishTime(null);
        draftDocument.setCreateBy("system");
        draftDocument.setUpdateBy("system");
        draftDocument.setCreateTime(LocalDateTime.now());
        draftDocument.setUpdateTime(LocalDateTime.now());
        draftDocument.setDeleted(0);
    }
    
    /**
     * 测试正常发布文档时应创建版本记录并更新状态
     */
    @Test
    void should_createVersionAndUpdateStatus_when_publishDraftDocument() {
        // Given: 准备草稿文档
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When: 发布文档
        documentService.publishDocument(testDocumentId);
        
        // Then: 验证创建了版本记录
        verify(versionMapper, times(1)).insert(any(KbDocumentVersion.class));
        
        // Then: 验证更新了文档状态
        verify(documentMapper, times(1)).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试发布文档时应设置发布时间
     */
    @Test
    void should_setPublishTime_when_publishDocument() {
        // Given
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When
        documentService.publishDocument(testDocumentId);
        
        // Then
        verify(documentMapper, times(1)).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试发布不存在的文档时应抛出NoSuchElementException
     */
    @Test
    void should_throwNoSuchElementException_when_publishNonExistentDocument() {
        // Given: 模拟文档不存在
        when(documentMapper.selectById(999999L)).thenReturn(null);
        
        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            documentService.publishDocument(999999L);
        });
        
        // Then: 验证没有调用后续操作
        verify(versionMapper, never()).insert(any(KbDocumentVersion.class));
        verify(documentMapper, never()).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试发布已发布的文档时应抛出IllegalStateException
     */
    @Test
    void should_throwIllegalStateException_when_publishAlreadyPublishedDocument() {
        // Given: 准备已发布状态的文档
        KbDocument publishedDoc = new KbDocument();
        publishedDoc.setId(testDocumentId);
        publishedDoc.setStatus("PUBLISHED");
        when(documentMapper.selectById(testDocumentId)).thenReturn(publishedDoc);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            documentService.publishDocument(testDocumentId);
        });
        
        // Then: 验证没有创建版本或更新文档
        verify(versionMapper, never()).insert(any(KbDocumentVersion.class));
        verify(documentMapper, never()).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试发布已下线的文档时应抛出IllegalStateException
     */
    @Test
    void should_throwIllegalStateException_when_publishOfflineDocument() {
        // Given: 准备已下线状态的文档
        KbDocument offlineDoc = new KbDocument();
        offlineDoc.setId(testDocumentId);
        offlineDoc.setStatus("OFFLINE");
        when(documentMapper.selectById(testDocumentId)).thenReturn(offlineDoc);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            documentService.publishDocument(testDocumentId);
        });
        
        // Then
        verify(versionMapper, never()).insert(any(KbDocumentVersion.class));
        verify(documentMapper, never()).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试多次发布应生成递增的版本号
     */
    @Test
    void should_incrementVersionNumber_when_publishMultipleTimes() {
        // Given: 第一次发布
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        documentService.publishDocument(testDocumentId);
        
        // Then: 验证第一次发布版本号为2
        verify(versionMapper, times(1)).insert(any(KbDocumentVersion.class));
        
        // Given: 准备第二次发布（模拟回滚后的草稿）
        KbDocument draftAgain = new KbDocument();
        draftAgain.setId(testDocumentId);
        draftAgain.setStatus("DRAFT");
        draftAgain.setVersion(2); // 回滚后版本号为2
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftAgain);
        
        // When: 第二次发布
        documentService.publishDocument(testDocumentId);
        
        // Then: 验证总共调用了两次 insert
        verify(versionMapper, times(2)).insert(any(KbDocumentVersion.class));
    }
    
    /**
     * 测试发布文档应保留原有内容到版本记录
     */
    @Test
    void should_preserveContentInVersion_when_publishDocument() {
        // Given
        String originalContent = "# 原始内容\n\n测试文本";
        draftDocument.setContent(originalContent);
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When
        documentService.publishDocument(testDocumentId);
        
        // Then: 验证调用了版本插入
        verify(versionMapper, times(1)).insert(any(KbDocumentVersion.class));
    }
    
    /**
     * 测试软删除文档
     */
    @Test
    void testSoftDeleteDocument() {
        // Given: Mock 文档存在
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When: 删除文档
        documentService.deleteDocument(testDocumentId);
        
        // Then: 验证调用了逻辑删除（MyBatis-Plus使用deleteById进行逻辑删除）
        verify(documentMapper, times(1)).deleteById(testDocumentId);
        
        // Then: 再次查询时返回null(模拟逻辑删除后的效果)
        when(documentMapper.selectById(testDocumentId)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> {
            documentService.getDocumentById(testDocumentId);
        });
    }
    
    /**
     * 测试版本回滚功能
     */
    @Test
    void testRollbackVersion() {
        // Given: Mock 文档存在，准备发布
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When: 先发布一次(版本2)
        documentService.publishDocument(testDocumentId);
        
        // Then: 验证创建了版本记录
        verify(versionMapper, times(1)).insert(any(KbDocumentVersion.class));
        
        // Given: Mock 版本列表和已发布文档
        KbDocument publishedDoc = new KbDocument();
        publishedDoc.setId(testDocumentId);
        publishedDoc.setCategoryId(testCategoryId);
        publishedDoc.setTitle("测试文档");
        publishedDoc.setContent("# 测试内容");
        publishedDoc.setStatus("PUBLISHED");
        publishedDoc.setVersion(2);
        publishedDoc.setPublishTime(LocalDateTime.now());
        
        KbDocumentVersion version1 = new KbDocumentVersion();
        version1.setId(1L);
        version1.setDocumentId(testDocumentId);
        version1.setVersion(1);
        version1.setTitle("测试文档");
        version1.setContent("# 测试内容");
        
        List<KbDocumentVersion> versions = new ArrayList<>();
        versions.add(version1);
        
        when(documentMapper.selectById(testDocumentId)).thenReturn(publishedDoc);
        when(versionMapper.selectById(1L)).thenReturn(version1); // Mock 根据ID查询版本
        
        // When: 回滚到版本1
        documentService.rollbackVersion(testDocumentId, 1L);
        
        // Then: 验证更新了文档
        verify(documentMapper, times(2)).updateById(any(KbDocument.class));
    }
    
    /**
     * 测试分页查询筛选条件
     */
    @Test
    void testPageDocumentsWithFilters() {
        // Given: Mock 分页查询结果
        Page<KbDocument> mockPage = new Page<>(1, 10);
        List<KbDocument> records = new ArrayList<>();
        
        KbDocument javaDoc = new KbDocument();
        javaDoc.setId(1L);
        javaDoc.setCategoryId(testCategoryId);
        javaDoc.setTitle("Java入门");
        javaDoc.setContent("# Java内容");
        javaDoc.setStatus("DRAFT");
        javaDoc.setVersion(1);
        records.add(javaDoc);
        
        mockPage.setRecords(records);
        mockPage.setTotal(1);
        
        when(documentMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
        
        // When: 按标题筛选
        DocumentQueryDTO query = new DocumentQueryDTO();
        query.setTitle("Java");
        query.setPageNum(1);
        query.setPageSize(10);
        
        Page<KbDocument> page = documentService.pageDocuments(query);
        
        // Then: 验证返回的记录不为空
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
        // Given: Mock 文档存在并准备发布
        when(documentMapper.selectById(testDocumentId)).thenReturn(draftDocument);
        
        // When: 发布文档
        documentService.publishDocument(testDocumentId);
        
        // Given: Mock 已发布状态的文档
        KbDocument publishedDoc = new KbDocument();
        publishedDoc.setId(testDocumentId);
        publishedDoc.setStatus("PUBLISHED");
        when(documentMapper.selectById(testDocumentId)).thenReturn(publishedDoc);
        
        // When & Then: 尝试更新已发布文档,应抛出异常
        assertThrows(IllegalStateException.class, () -> {
            documentService.updateDocument(testDocumentId, "新标题", "新内容");
        });
    }
}
