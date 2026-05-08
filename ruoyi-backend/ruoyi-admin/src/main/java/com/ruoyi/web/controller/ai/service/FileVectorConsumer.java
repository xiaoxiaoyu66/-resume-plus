package com.ruoyi.web.controller.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件向量化消息消费者
 * 注意：此服务需要 RabbitMQ 依赖
 * 如需使用，请确保 RabbitMQ 已配置
 */
@Component
public class FileVectorConsumer {

    private static final Logger log = LoggerFactory.getLogger(FileVectorConsumer.class);

    /**
     * 处理文件向量化消息
     */
    public void processFileVectorization(String message) {
        log.debug("处理文件向量化消息: {}", message);
    }
}
