package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.config.EmbeddingProperties;
import jakarta.annotation.Resource;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Embedding 服务
 * 调用 SiliconFlow API 将文本转换为向量
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Resource
    private EmbeddingProperties embeddingProperties;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 单文本转向量
     *
     * @param text 输入文本
     * @return 向量数组
     */
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 批量文本转向量
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤空文本
        List<String> validTexts = texts.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .toList();

        if (validTexts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", embeddingProperties.getModel());
            requestBody.put("input", validTexts);
            requestBody.put("encoding_format", "float");
            // 指定向量维度（BAAI/bge-m3 支持通过 dimensions 参数控制输出维度）
            requestBody.put("dimensions", embeddingProperties.getDimension());

            // 构建请求
            Request request = new Request.Builder()
                    .url(embeddingProperties.getApiUrl())
                    .header("Authorization", "Bearer " + embeddingProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(
                            requestBody.toJSONString(),
                            MediaType.parse("application/json")
                    ))
                    .build();

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Embedding API 调用失败: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("Embedding API 调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                // 解析向量
                List<float[]> embeddings = new ArrayList<>();
                var dataArray = jsonResponse.getJSONArray("data");
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    var embeddingArray = item.getJSONArray("embedding");
                    float[] vector = new float[embeddingArray.size()];
                    for (int j = 0; j < embeddingArray.size(); j++) {
                        vector[j] = embeddingArray.getFloatValue(j);
                    }
                    embeddings.add(vector);
                }

                log.info("Embedding 成功: {} 条文本", embeddings.size());
                return embeddings;
            }

        } catch (IOException e) {
            log.error("Embedding API 调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding API 调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 文本分块
     *
     * @param text      原始文本
     * @param chunkSize 每块大小（字符数）
     * @param overlap   重叠大小
     * @return 分块后的文本列表
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 按段落分割
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            // 如果当前块加上新段落超过限制，保存当前块
            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // 保留重叠部分
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = currentChunk.substring(currentChunk.length() - overlap);
                    currentChunk = new StringBuilder(overlapText);
                } else {
                    currentChunk = new StringBuilder();
                }
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("文本分块完成: {} 块", chunks.size());
        return chunks;
    }

    /**
     * 智能分块（按句子边界）
     *
     * @param text      原始文本
     * @param chunkSize 目标块大小
     * @return 分块后的文本列表
     */
    public List<String> chunkTextSmart(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 按句子分割（简单实现，按标点符号）
        String[] sentences = text.split("(?<=[。！？.!?])");

        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            // 如果当前块加上新句子超过限制，保存当前块
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }

            currentChunk.append(sentence);
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("智能分块完成: {} 块", chunks.size());
        return chunks;
    }
}
