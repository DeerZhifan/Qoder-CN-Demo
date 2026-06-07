package com.cdp.kb.controller;

import com.cdp.kb.common.Result;
import com.cdp.kb.entity.DocumentVersion;
import com.cdp.kb.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/versions")
@RequiredArgsConstructor
public class VersionController {
    
    private final VersionService versionService;
    
    /**
     * 获取文档的版本列表
     */
    @GetMapping
    public Result<List<DocumentVersion>> listVersions(@PathVariable Long documentId) {
        List<DocumentVersion> versions = versionService.listVersions(documentId);
        return Result.success(versions);
    }
    
    /**
     * 获取指定版本的内容
     */
    @GetMapping("/{version}")
    public Result<DocumentVersion> getVersion(@PathVariable Long documentId, 
                                               @PathVariable Integer version) {
        DocumentVersion docVersion = versionService.getVersion(documentId, version);
        if (docVersion == null) {
            return Result.error("版本不存在");
        }
        return Result.success(docVersion);
    }
}
