package com.ruoyi.web.controller.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 服务配置属性
 */
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    private String apiUrl = "https://api.siliconflow.cn/v1/embeddings";
    private String apiKey = "";
    private String model = "BAAI/bge-m3";
    private int dimension = 768;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
