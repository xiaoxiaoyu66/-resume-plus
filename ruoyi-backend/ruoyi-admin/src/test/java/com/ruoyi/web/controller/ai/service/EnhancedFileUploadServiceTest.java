package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.service.ElasticsearchFileService.FileSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版文件上传服务集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class EnhancedFileUploadServiceTest {

    @Autowired
    private EnhancedFileUploadService enhancedFileUploadService;

    @Test
    void testUploadAndProcessFile() throws IOException {
        // 创建模拟文件
        String content = "这是一个测试文档，包含 Java 和 Spring Boot 相关内容";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-java.pdf",
                "application/pdf",
                content.getBytes()
        );

        // 上传文件
        Map<String, Object> result = enhancedFileUploadService.uploadAndProcessFile(file, 1L);

        // 验证结果
        assertNotNull(result, "上传结果不应该为空");
        assertNotNull(result.get("fileId"), "文件ID不应该为空");
        assertEquals("test-java.pdf", result.get("originalName"), "文件名应该匹配");
        assertTrue((Boolean) result.get("indexed"), "文件应该被索引到 ES");
        assertTrue((Boolean) result.get("vectorizationQueued"), "向量化任务应该被发送到队列");

        Long fileId = (Long) result.get("fileId");

        // 等待 ES 索引刷新
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 测试搜索
        List<FileSearchResult> searchResults = enhancedFileUploadService.searchFiles("Java", 1L, 1, 10);
        assertFalse(searchResults.isEmpty(), "搜索结果不应该为空");

        // 清理
        enhancedFileUploadService.deleteFile(fileId, (String) result.get("fileName"), 1L);
    }

    @Test
    void testSearchFiles() {
        // 搜索测试
        List<FileSearchResult> results = enhancedFileUploadService.searchFiles("Spring Boot", 1L, 1, 10);
        assertNotNull(results, "搜索结果不应该为 null");
    }
}
