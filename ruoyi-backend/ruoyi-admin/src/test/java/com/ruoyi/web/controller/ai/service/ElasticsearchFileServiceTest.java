package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.service.ElasticsearchFileService.FileSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elasticsearch 文件搜索服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class ElasticsearchFileServiceTest {

    @Autowired
    private ElasticsearchFileService elasticsearchFileService;

    @BeforeEach
    void setUp() {
        // 初始化索引
        elasticsearchFileService.initIndex();
    }

    @Test
    void testIndexAndSearch() {
        // 准备测试数据
        Long fileId = 999L;
        Long userId = 1L;
        String fileName = "测试文档.pdf";
        String content = "这是一个测试文档，包含 Java 和 Spring Boot 相关内容";
        String fileType = "pdf";

        // 索引文件
        elasticsearchFileService.indexFile(fileId, userId, fileName, content, fileType);

        // 等待索引刷新
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 搜索
        List<FileSearchResult> results = elasticsearchFileService.search("Java", userId, 1, 10);

        // 验证结果
        assertFalse(results.isEmpty(), "搜索结果不应该为空");
        assertEquals(fileId, results.get(0).getFileId(), "文件ID应该匹配");
        assertEquals(fileName, results.get(0).getFileName(), "文件名应该匹配");

        // 清理
        elasticsearchFileService.deleteIndex(fileId);
    }

    @Test
    void testSearchWithHighlight() {
        // 索引测试文档
        elasticsearchFileService.indexFile(
                1000L, 1L, "简历.pdf",
                "精通 Java 开发，熟悉 Spring Boot 框架，有 3 年开发经验",
                "pdf"
        );

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 搜索
        List<FileSearchResult> results = elasticsearchFileService.search("Spring Boot", 1L, 1, 10);

        // 验证高亮
        assertFalse(results.isEmpty(), "搜索结果不应该为空");
        FileSearchResult result = results.get(0);
        assertNotNull(result.getHighlight(), "高亮内容不应该为空");
        assertTrue(result.getHighlight().contains("<mark>"), "高亮内容应该包含 mark 标签");

        // 清理
        elasticsearchFileService.deleteIndex(1000L);
    }

    @Test
    void testDeleteIndex() {
        // 索引文件
        elasticsearchFileService.indexFile(1001L, 1L, "temp.pdf", "临时内容", "pdf");

        // 删除索引
        elasticsearchFileService.deleteIndex(1001L);

        // 等待删除生效
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 搜索应该返回空
        List<FileSearchResult> results = elasticsearchFileService.search("临时", 1L, 1, 10);
        assertTrue(results.isEmpty(), "删除后搜索结果应该为空");
    }
}
