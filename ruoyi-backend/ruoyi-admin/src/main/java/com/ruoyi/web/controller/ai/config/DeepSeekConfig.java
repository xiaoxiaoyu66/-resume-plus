package com.ruoyi.web.controller.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek AI 配置
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /** API密钥 */
    private String apiKey;

    /** API地址 */
    private String apiUrl;

    /** 模型名称 */
    private String model;

    /** 最大token数 */
    private Integer maxTokens = 2048;

    /** 温度参数 */
    private Double temperature = 0.7;

    /** System Prompt 配置 */
    private Map<String, String> systemPrompt = new HashMap<>();

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Map<String, String> getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(Map<String, String> systemPrompt) { this.systemPrompt = systemPrompt; }

    /**
     * 获取指定场景的System Prompt
     * @param scene 场景：default/resume/interview-hr/interview-pro/career
     * @return System Prompt
     */
    public String getSystemPromptByScene(String scene) {
        if (scene == null || scene.isEmpty()) {
            scene = "default";
        }
        return systemPrompt.getOrDefault(scene, systemPrompt.getOrDefault("default",
                "你是历时诊智能求职助手，专注于帮助用户分析简历、优化求职材料、提供面试辅导和职业规划建议。"));
    }
}
