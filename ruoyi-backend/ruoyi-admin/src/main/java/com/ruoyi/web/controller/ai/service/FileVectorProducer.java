package com.ruoyi.web.controller.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * 文件向量化消息生产者
 * 注意：此服务需要 RabbitMQ 依赖
 * 如需使用，请确保 RabbitMQ 已配置
 */
@Service
public class FileVectorProducer {

    private static final Logger log = LoggerFactory.getLogger(FileVectorProducer.class);

    /**
     * 发送文件向量化消息
     *
     * @param fileId   文件ID
     * @param userId   用户ID
     * @param fileName 文件名
     * @param content  文件内容
     */
    public void sendFileForVectorization(Long fileId, Long userId, String fileName, String content) {
        log.debug("发送文件向量化消息: fileId={}, fileName={}", fileId, fileName);
    }

    /**
     * 文件向量化消息
     */
    public static class FileVectorMessage implements Serializable {
        private Long fileId;
        private Long userId;
        private String fileName;
        private String content;

        // Getters and Setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
