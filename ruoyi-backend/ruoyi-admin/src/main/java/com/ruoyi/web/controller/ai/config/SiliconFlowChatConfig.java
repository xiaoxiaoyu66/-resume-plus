package com.ruoyi.web.controller.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SiliconFlow Chat 配置（AI 模型兜底）
 * 复用已有的 EMBEDDING_API_KEY，无需额外申请
 */
@Configuration
@ConfigurationProperties(prefix = "siliconflow.chat")
public class SiliconFlowChatConfig {

    /** API 地址 */
    private String apiUrl;

    /** API 密钥（复用 embedding 的 key） */
    private String apiKey;

    /** 模型名称 */
    private String model;

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
