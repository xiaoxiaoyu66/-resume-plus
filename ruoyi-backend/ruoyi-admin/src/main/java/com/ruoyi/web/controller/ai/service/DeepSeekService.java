package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek AI 对话服务
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    @Resource
    private DeepSeekConfig deepSeekConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 调用 DeepSeek API（非流式）
     *
     * @param messages 消息列表（格式：[{role: "user"/"assistant", content: "..."}]
     * @return AI 回复内容
     */
    public String callDeepSeek(JSONArray messages) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("temperature", deepSeekConfig.getTemperature());

        Request request = new Request.Builder()
                .url(deepSeekConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toJSONString()))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown";
                log.error("DeepSeek API error: {} - {}", response.code(), errorBody);
                throw new RuntimeException("DeepSeek API 调用失败: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject json = JSON.parseObject(responseBody);

            JSONArray choices = json.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    return message.getString("content");
                }
            }
            return "";
        } catch (IOException e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("AI服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 构建上下文消息列表
     *
     * @param systemPrompt 系统提示词
     * @param historyMessages 历史消息（格式：List<ChatMessage>）
     * @param userMessage 当前用户提问
     * @return JSONArray
     */
    public JSONArray buildMessages(String systemPrompt,
                                   java.util.List<com.ruoyi.web.controller.ai.domain.ChatMessage> historyMessages,
                                   String userMessage) {
        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }
        if (historyMessages != null) {
            for (com.ruoyi.web.controller.ai.domain.ChatMessage msg : historyMessages) {
                JSONObject m = new JSONObject();
                m.put("role", msg.getRole() == 1 ? "user" : "assistant");
                m.put("content", msg.getContent());
                messages.add(m);
            }
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        return messages;
    }

    /**
     * 简单对话接口（直接传入问题）
     *
     * @param question 用户问题
     * @return AI 回答
     */
    public String chat(String question) {
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);
        return callDeepSeek(messages);
    }
}
