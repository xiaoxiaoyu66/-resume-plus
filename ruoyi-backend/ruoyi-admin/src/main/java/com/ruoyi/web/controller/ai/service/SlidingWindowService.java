package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.web.controller.ai.config.AiConstants;
import com.ruoyi.web.controller.ai.domain.ChatMessage;
import com.ruoyi.web.controller.ai.mapper.ChatMessageMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口记忆服务
 * 管理对话上下文窗口，支持Token限制和智能截断
 */
@Service
public class SlidingWindowService {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowService.class);

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private RedisCache redisCache;

    // 配置参数
    @Value("${ai.window.max-rounds:5}")
    private int maxRounds; // 最大保留轮数

    @Value("${ai.window.max-tokens:3000}")
    private int maxTokens; // 最大Token数

    @Value("${ai.window.system-priority:true}")
    private boolean systemPriority; // 系统提示词优先保留

    private static final String SESSION_CONTEXT_PREFIX = AiConstants.SESSION_CONTEXT_PREFIX;
    private static final int TOKEN_PER_CHAR = AiConstants.TOKEN_PER_CHAR; // 粗略估算：4字符 ≈ 1 Token

    /**
     * 获取会话上下文（滑动窗口）
     *
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词
     * @return 符合窗口限制的消息列表
     */
    public List<ChatMessage> getContextWindow(Long sessionId, String systemPrompt) {
        String cacheKey = SESSION_CONTEXT_PREFIX + sessionId;
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<ChatMessage> cachedContext = redisCache.getCacheObject(cacheKey);
        if (cachedContext != null) {
            log.debug("从缓存获取会话上下文: sessionId={}", sessionId);
            return cachedContext;
        }

        // 从数据库查询
        List<ChatMessage> allMessages = messageMapper.selectMessagesBySessionId(sessionId);
        if (allMessages == null || allMessages.isEmpty()) {
            return new ArrayList<>();
        }

        // 应用滑动窗口策略
        List<ChatMessage> windowedMessages = applySlidingWindow(allMessages, systemPrompt);

        // 缓存结果（5分钟过期）
        redisCache.setCacheObject(cacheKey, windowedMessages, 5, TimeUnit.MINUTES);

        return windowedMessages;
    }

    /**
     * 应用滑动窗口策略
     *
     * @param messages 所有消息
     * @param systemPrompt 系统提示词（用于估算Token）
     * @return 窗口内的消息
     */
    private List<ChatMessage> applySlidingWindow(List<ChatMessage> messages, String systemPrompt) {
        // 1. 按轮数限制
        int maxMessages = maxRounds * 2; // 每轮包含用户+AI两条
        int startIndex = Math.max(0, messages.size() - maxMessages);
        List<ChatMessage> windowed = new ArrayList<>(
                messages.subList(startIndex, messages.size())
        );

        // 2. 按Token数限制（粗略估算）
        int systemTokens = estimateTokens(systemPrompt);
        int availableTokens = maxTokens - systemTokens;
        
        List<ChatMessage> tokenLimited = new ArrayList<>();
        int currentTokens = 0;
        
        // 从后往前遍历，优先保留最近的对话
        for (int i = windowed.size() - 1; i >= 0; i--) {
            ChatMessage msg = windowed.get(i);
            int msgTokens = estimateTokens(msg.getContent());
            
            if (currentTokens + msgTokens <= availableTokens) {
                tokenLimited.add(0, msg); // 插入到头部保持顺序
                currentTokens += msgTokens;
            } else {
                // Token超限，停止添加
                log.debug("Token限制触发，截断历史消息: sessionId={}, 保留{}/{}条", 
                        msg.getSessionId(), tokenLimited.size(), windowed.size());
                break;
            }
        }

        return tokenLimited;
    }

    /**
     * 估算Token数（粗略）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 粗略估算：英文约4字符/Token，中文约2字符/Token
        // 这里使用保守估计
        return text.length() / TOKEN_PER_CHAR + 1;
    }

    /**
     * 添加消息到上下文并更新窗口
     *
     * @param sessionId 会话ID
     * @param message 新消息
     */
    public void addMessageToContext(Long sessionId, ChatMessage message) {
        String cacheKey = SESSION_CONTEXT_PREFIX + sessionId;
        
        @SuppressWarnings("unchecked")
        List<ChatMessage> context = redisCache.getCacheObject(cacheKey);
        if (context == null) {
            context = new ArrayList<>();
        }
        
        context.add(message);
        
        // 如果超出窗口限制，移除最旧的消息
        if (context.size() > maxRounds * 2) {
            context.remove(0);
        }
        
        // 更新缓存
        redisCache.setCacheObject(cacheKey, context, 5, TimeUnit.MINUTES);
    }

    /**
     * 清空会话上下文缓存
     *
     * @param sessionId 会话ID
     */
    public void clearContext(Long sessionId) {
        String cacheKey = SESSION_CONTEXT_PREFIX + sessionId;
        redisCache.deleteObject(cacheKey);
        log.debug("清空会话上下文缓存: sessionId={}", sessionId);
    }

    /**
     * 获取带Token统计的上下文
     *
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词
     * @return ContextResult 包含消息和Token统计
     */
    public ContextResult getContextWithStats(Long sessionId, String systemPrompt) {
        List<ChatMessage> messages = getContextWindow(sessionId, systemPrompt);
        
        int systemTokens = estimateTokens(systemPrompt);
        int contextTokens = messages.stream()
                .mapToInt(m -> estimateTokens(m.getContent()))
                .sum();
        
        return new ContextResult(messages, systemTokens, contextTokens);
    }

    /**
     * 上下文结果
     */
    public static class ContextResult {
        private final List<ChatMessage> messages;
        private final int systemTokens;
        private final int contextTokens;

        public ContextResult(List<ChatMessage> messages, int systemTokens, int contextTokens) {
            this.messages = messages;
            this.systemTokens = systemTokens;
            this.contextTokens = contextTokens;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }

        public int getSystemTokens() {
            return systemTokens;
        }

        public int getContextTokens() {
            return contextTokens;
        }

        public int getTotalTokens() {
            return systemTokens + contextTokens;
        }
    }
}
