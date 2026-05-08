package com.ruoyi.web.controller.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API服务实现
 * 所有API调用都在后端完成，API密钥不会暴露给前端
 *
 * @author AI Team
 */
@Service
public class DeepSeekServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImpl.class);

    @Resource
    private DeepSeekConfig deepSeekConfig;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    /**
     * 非流式对话
     *
     * @param question 用户问题
     * @return AI回答
     */
    public String chat(String question) {
        try {
            log.info("调用DeepSeek API，问题: {}", question.substring(0, Math.min(50, question.length())));

            Map<String, Object> requestBody = buildRequestBody(question, false);

            Request request = new Request.Builder()
                    .url(deepSeekConfig.getApiUrl())
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(JSON.toJSONString(requestBody), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API调用失败: {}", response.code());
                    throw new RuntimeException("AI服务调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                return parseResponse(jsonResponse);
            }

        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String question, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("stream", stream);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", deepSeekConfig.getTemperature());

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", question);
        messages.add(message);

        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * 解析非流式响应
     */
    private String parseResponse(JSONObject response) {
        if (response.containsKey("choices") && !response.getJSONArray("choices").isEmpty()) {
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            if (choice.containsKey("message")) {
                return choice.getJSONObject("message").getString("content");
            }
        }
        return "抱歉，我无法理解您的问题";
    }
}
