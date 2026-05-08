package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * SSE 流式对话服务
 * 支持实时流式输出和打字机效果
 */
@Service
public class SseChatService {

    private static final Logger log = LoggerFactory.getLogger(SseChatService.class);

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Resource(name = "aiTaskExecutor")
    private ThreadPoolTaskExecutor aiTaskExecutor;

    @Resource
    private ModelRouter modelRouter;

    /**
     * 流式调用 DeepSeek API
     *
     * @param messages 消息列表
     * @param emitter SSE发射器
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    public void streamChat(JSONArray messages, SseEmitter emitter,
                          OnCompleteCallback onComplete,
                          OnErrorCallback onError) {
        streamChat(messages, emitter, onComplete, onError, null);
    }

    /**
     * 流式调用 DeepSeek API（带 sessionId）
     *
     * @param messages 消息列表
     * @param emitter SSE发射器
     * @param onComplete 完成回调
     * @param onError 错误回调
     * @param sessionId 会话ID
     */
    public void streamChat(JSONArray messages, SseEmitter emitter,
                          OnCompleteCallback onComplete,
                          OnErrorCallback onError,
                          Long sessionId) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);

        // 发送开始事件
        try {
            emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"status\":\"started\"}"));
        } catch (IOException e) {
            log.error("发送开始事件失败", e);
        }

        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // 由 ModelRouter 自动路由：主 DeepSeek → 故障切换 SiliconFlow
        aiTaskExecutor.execute(() -> {
            try (Response response = modelRouter.call(requestBody)) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("DeepSeek API error: {} - {}", response.code(), errorBody);
                    handleError(emitter, "API调用失败: " + response.code(), onError);
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        handleError(emitter, "响应体为空", onError);
                        return;
                    }

                    String line;
                    while ((line = responseBody.source().readUtf8Line()) != null) {
                        if (line.isEmpty()) continue;

                        // SSE 格式: data: {...}
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);

                            // 流结束标记
                            if ("[DONE]".equals(data)) {
                                isCompleted.set(true);
                                break;
                            }

                            try {
                                JSONObject chunk = JSON.parseObject(data);
                                JSONArray choices = chunk.getJSONArray("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    JSONObject choice = choices.getJSONObject(0);
                                    JSONObject delta = choice.getJSONObject("delta");
                                    if (delta != null) {
                                        String content = delta.getString("content");
                                        if (content != null && !content.isEmpty()) {
                                            fullResponse.append(content);

                                            // 发送 token 事件
                                            JSONObject tokenData = new JSONObject();
                                            tokenData.put("token", content);
                                            tokenData.put("index", fullResponse.length() - content.length());

                                            try {
                                                emitter.send(SseEmitter.event()
                                                        .name("token")
                                                        .data(tokenData.toJSONString()));
                                            } catch (Exception e) {
                                                log.error("发送 token 事件失败", e);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("解析 SSE 数据块失败: {}", data);
                            }
                        }
                    }

                    // 发送完成事件
                    if (isCompleted.get()) {
                        JSONObject completeData = new JSONObject();
                        completeData.put("status", "completed");
                        completeData.put("fullContent", fullResponse.toString());
                        if (sessionId != null) {
                            completeData.put("sessionId", sessionId);
                        }

                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(completeData.toJSONString()));

                        emitter.complete();

                        if (onComplete != null) {
                            onComplete.onComplete(fullResponse.toString());
                        }
                    }

                } catch (Exception e) {
                    log.error("处理流式响应失败", e);
                    handleError(emitter, e.getMessage(), onError);
                }
            } catch (IOException e) {
                log.error("DeepSeek 流式请求失败", e);
                handleError(emitter, e.getMessage(), onError);
            }
        });
    }

    /**
     * 处理错误
     */
    private void handleError(SseEmitter emitter, String errorMsg, OnErrorCallback onError) {
        try {
            JSONObject errorData = new JSONObject();
            errorData.put("status", "error");
            errorData.put("message", errorMsg);
            
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorData.toJSONString()));
            emitter.completeWithError(new RuntimeException(errorMsg));
        } catch (IOException e) {
            log.error("发送错误事件失败", e);
        }
        
        if (onError != null) {
            onError.onError(errorMsg);
        }
    }

    /**
     * 完成回调接口
     */
    @FunctionalInterface
    public interface OnCompleteCallback {
        void onComplete(String fullContent);
    }

    /**
     * 错误回调接口
     */
    @FunctionalInterface
    public interface OnErrorCallback {
        void onError(String errorMsg);
    }
}
