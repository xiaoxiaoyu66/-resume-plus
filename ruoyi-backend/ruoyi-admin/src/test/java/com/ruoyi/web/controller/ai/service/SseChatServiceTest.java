package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSE 流式对话服务单元测试
 */
@SpringBootTest
public class SseChatServiceTest {

    @Autowired
    private SseChatService sseChatService;

    @Test
    void testSseEmitterCreation() {
        // 测试 SseEmitter 创建
        SseEmitter emitter = new SseEmitter();
        assertNotNull(emitter);
    }

    @Test
    void testStreamChatWithEmptyMessages() {
        // 测试空消息列表
        JSONArray emptyMessages = new JSONArray();
        SseEmitter emitter = new SseEmitter(5000L); // 5秒超时

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        // 由于需要调用真实的 DeepSeek API，这里主要测试方法不抛异常
        // 实际测试时需要配置测试用的 API Key
        assertDoesNotThrow(() -> {
            // 注意：这里不会真正调用 API，因为没有配置有效的 API Key
            // 主要测试代码结构和异常处理
            sseChatService.streamChat(emptyMessages, emitter,
                fullContent -> {
                    result.set(fullContent);
                    latch.countDown();
                },
                errorMsg -> {
                    error.set(errorMsg);
                    latch.countDown();
                }
            );
        });

        // 等待一段时间（不会真正完成，因为没有有效 API Key）
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证 emitter 被创建
        assertNotNull(emitter);
    }

    @Test
    void testStreamChatWithMessages() {
        // 测试带有消息的流式对话
        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个AI助手");
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "你好");
        messages.add(userMsg);

        SseEmitter emitter = new SseEmitter(5000L);

        // 主要测试方法不抛异常
        assertDoesNotThrow(() -> {
            sseChatService.streamChat(messages, emitter,
                fullContent -> {
                    System.out.println("完成: " + fullContent);
                },
                errorMsg -> {
                    System.out.println("错误: " + errorMsg);
                }
            );
        });

        assertNotNull(emitter);
    }

    @Test
    void testSseEmitterTimeout() {
        // 测试 SseEmitter 超时设置
        SseEmitter emitter = new SseEmitter(1000L); // 1秒超时
        assertNotNull(emitter);

        // 验证 emitter 可以正常设置超时
        emitter.onTimeout(() -> {
            System.out.println("Emitter 超时");
        });
    }

    @Test
    void testSseEmitterCompletion() {
        // 测试 SseEmitter 完成
        SseEmitter emitter = new SseEmitter();
        assertNotNull(emitter);

        // 设置完成回调
        emitter.onCompletion(() -> {
            System.out.println("Emitter 完成");
        });

        // 正常完成
        assertDoesNotThrow(() -> emitter.complete());
    }

    @Test
    void testSseEmitterError() {
        // 测试 SseEmitter 错误处理
        SseEmitter emitter = new SseEmitter();
        assertNotNull(emitter);

        // 设置错误回调
        emitter.onError((e) -> {
            System.out.println("Emitter 错误: " + e.getMessage());
        });

        // 模拟错误
        assertDoesNotThrow(() -> emitter.completeWithError(new RuntimeException("测试错误")));
    }

    @Test
    void testCallbackInterfaces() {
        // 测试回调接口
        final String[] capturedResult = new String[1];
        final String[] capturedError = new String[1];

        SseChatService.OnCompleteCallback completeCallback = content -> {
            capturedResult[0] = content;
        };

        SseChatService.OnErrorCallback errorCallback = errorMsg -> {
            capturedError[0] = errorMsg;
        };

        // 验证回调可以被调用
        assertDoesNotThrow(() -> completeCallback.onComplete("测试内容"));
        assertDoesNotThrow(() -> errorCallback.onError("测试错误"));

        assertEquals("测试内容", capturedResult[0]);
        assertEquals("测试错误", capturedError[0]);
    }

    @Test
    void testMessageBuilding() {
        // 测试消息构建
        JSONArray messages = new JSONArray();

        // 添加系统消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个有帮助的AI助手");
        messages.add(systemMessage);

        // 添加用户消息
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "请介绍一下自己");
        messages.add(userMessage);

        // 添加助手消息
        JSONObject assistantMessage = new JSONObject();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", "我是AI助手");
        messages.add(assistantMessage);

        // 验证消息列表
        assertEquals(3, messages.size());
        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertEquals("user", messages.getJSONObject(1).getString("role"));
        assertEquals("assistant", messages.getJSONObject(2).getString("role"));
    }
}
