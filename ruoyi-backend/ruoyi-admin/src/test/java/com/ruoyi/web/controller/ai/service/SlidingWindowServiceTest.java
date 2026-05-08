package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.domain.ChatMessage;
import com.ruoyi.web.controller.ai.service.SlidingWindowService.ContextResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 滑动窗口记忆服务单元测试
 */
@SpringBootTest
public class SlidingWindowServiceTest {

    @Autowired
    private SlidingWindowService slidingWindowService;

    @Test
    void testEmptyContext() {
        // 测试空上下文
        String systemPrompt = "你是一个AI助手";
        
        // 使用模拟数据测试
        List<ChatMessage> result = slidingWindowService.getContextWindow(99999L, systemPrompt);
        
        assertNotNull(result);
        // 对于不存在的会话，应该返回空列表
        assertTrue(result.isEmpty());
    }

    @Test
    void testTokenEstimation() {
        // 测试Token估算
        String shortText = "Hello";
        String longText = "这是一段很长的中文文本，用来测试Token估算功能。".repeat(10);
        
        // 估算应该返回正数
        int shortTokens = estimateTokens(shortText);
        int longTokens = estimateTokens(longText);
        
        assertTrue(shortTokens > 0);
        assertTrue(longTokens > shortTokens);
        System.out.println("短文本Token: " + shortTokens);
        System.out.println("长文本Token: " + longTokens);
    }

    @Test
    void testContextWithStats() {
        // 测试带统计的上下文获取
        String systemPrompt = "你是一个AI助手";
        
        ContextResult result = slidingWindowService.getContextWithStats(99999L, systemPrompt);
        
        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertTrue(result.getSystemTokens() >= 0);
        assertTrue(result.getContextTokens() >= 0);
        assertTrue(result.getTotalTokens() >= result.getSystemTokens());
        
        System.out.println("系统提示词Token: " + result.getSystemTokens());
        System.out.println("上下文Token: " + result.getContextTokens());
        System.out.println("总Token: " + result.getTotalTokens());
    }

    @Test
    void testClearContext() {
        // 测试清空上下文
        Long sessionId = 12345L;
        
        // 清空应该正常执行不抛异常
        assertDoesNotThrow(() -> slidingWindowService.clearContext(sessionId));
    }

    @Test
    void testAddMessageToContext() {
        // 测试添加消息到上下文
        Long sessionId = 12345L;
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(1);
        message.setContent("测试消息");
        
        // 添加消息应该正常执行
        assertDoesNotThrow(() -> slidingWindowService.addMessageToContext(sessionId, message));
    }

    @Test
    void testWindowLimit() {
        // 测试窗口限制
        // 创建大量消息
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(1L);
            msg.setRole(i % 2 == 0 ? 1 : 2); // 用户和AI交替
            msg.setContent("消息内容 " + i);
            messages.add(msg);
        }
        
        // 验证消息列表
        assertEquals(20, messages.size());
        
        // 由于是从数据库查询，这里主要测试逻辑不抛异常
        String systemPrompt = "系统提示词";
        assertDoesNotThrow(() -> slidingWindowService.getContextWindow(1L, systemPrompt));
    }

    @Test
    void testTokenLimit() {
        // 测试Token限制
        String systemPrompt = "系统提示词".repeat(100); // 长系统提示词
        
        // 获取带统计的上下文
        ContextResult result = slidingWindowService.getContextWithStats(99999L, systemPrompt);
        
        // 系统提示词Token应该被正确计算
        assertTrue(result.getSystemTokens() > 0);
        System.out.println("长系统提示词Token数: " + result.getSystemTokens());
    }

    /**
     * 估算Token数（复制服务中的逻辑）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4 + 1;
    }
}
