package com.ruoyi.web.controller.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件向量化消息生产者测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class FileVectorProducerTest {

    @Autowired
    private FileVectorProducer fileVectorProducer;

    @Test
    void testSendVectorTask() {
        // 准备测试数据
        Long fileId = 999L;
        String fileName = "测试文件.pdf";
        Long userId = 1L;

        // 发送消息
        assertDoesNotThrow(() -> {
            fileVectorProducer.sendFileForVectorization(fileId, userId, fileName, "测试内容");
        }, "发送消息不应该抛出异常");
    }

    @Test
    void testMessageConverter() {
        // 测试消息转换器
        FileVectorProducer.FileVectorMessage message = new FileVectorProducer.FileVectorMessage();
        message.setFileId(100L);
        message.setFileName("test.pdf");
        message.setUserId(1L);

        // 验证消息对象
        assertEquals(100L, message.getFileId());
        assertEquals("test.pdf", message.getFileName());
        assertEquals(1L, message.getUserId());
    }
}
