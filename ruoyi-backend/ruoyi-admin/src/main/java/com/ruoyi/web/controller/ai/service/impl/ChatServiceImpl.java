package com.ruoyi.web.controller.ai.service.impl;

import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.web.controller.ai.config.AiConstants;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;
import com.ruoyi.web.controller.ai.domain.ChatMessage;
import com.ruoyi.web.controller.ai.domain.ChatSession;
import com.ruoyi.web.controller.ai.mapper.ChatMessageMapper;
import com.ruoyi.web.controller.ai.mapper.ChatSessionMapper;
import com.ruoyi.web.controller.ai.service.IChatService;
import com.ruoyi.web.controller.ai.service.DeepSeekService;
import com.ruoyi.web.controller.ai.service.FileContentService;
import com.ruoyi.web.controller.ai.service.FileContentService.FileContent;
import com.ruoyi.web.controller.ai.service.VectorService;
import com.ruoyi.web.controller.ai.service.VectorService.VectorSearchResult;

import java.time.Duration;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI对话Service实现
 * 使用真实的DeepSeek API
 *
 * @author AI Team
 */
@Service
public class ChatServiceImpl implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Resource
    private ChatSessionMapper sessionMapper;

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private FileContentService fileContentService;

    @Resource
    private VectorService vectorService;

    @Resource
    private RedisCache redisCache;

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chat(Long userId, Long sessionId, String message, List<String> fileNames) {
        return chatWithScene(userId, sessionId, message, fileNames, "default");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chatWithScene(Long userId, Long sessionId, String message, List<String> fileNames, String scene) {
        // 1. 检查限额
        checkQuota(userId);

        // 2. 创建或获取会话
        ChatSession session = getOrCreateSession(userId, sessionId, message);

        // 3. 查询历史上下文（保留最近5轮对话 = 10条消息）
        List<ChatMessage> history = messageMapper.selectMessagesBySessionId(session.getId());
        int ctxRounds = 5; // 保留5轮对话
        int ctxSize = ctxRounds * 2; // 每轮包含用户+AI两条消息
        if (history.size() > ctxSize) {
            history = history.subList(history.size() - ctxSize, history.size());
        }

        // 4. 如果有关联文件，使用向量检索获取相关内容
        String fullMessage = message;
        if (fileNames != null && !fileNames.isEmpty()) {
            // 方式1：向量检索（推荐）
            List<VectorSearchResult> searchResults = vectorService.searchSimilar(message, userId, 5);
            
            if (!searchResults.isEmpty()) {
                // 使用向量检索结果
                StringBuilder contextBuilder = new StringBuilder("\n\n请参考以下检索到的相关内容回答用户问题。基于这些内容组织回答，忽略无关内容，用自己的知识补充不足。\n");
                for (int i = 0; i < searchResults.size(); i++) {
                    VectorSearchResult result = searchResults.get(i);
                    contextBuilder.append("--- 相关内容 ").append(i + 1).append(" (相似度: ")
                            .append(String.format("%.2f", result.getSimilarity())).append(") ---\n")
                            .append(result.getChunkText()).append("\n\n");
                }
                contextBuilder.append("【用户问题】").append(message);
                fullMessage = contextBuilder.toString();
                
                log.info("使用向量检索: 找到{}条相关内容", searchResults.size());
            } else {
                // 方式2：回退到全文解析（兼容旧文件）
                List<FileContent> fileContents = fileContentService.parseFiles(fileNames);
                StringBuilder filePart = new StringBuilder("\n\n用户上传了以下文件，请基于文件内容回答用户的问题。如果文件中找不到相关信息，请如实告知。\n");
                for (FileContent fc : fileContents) {
                    filePart.append("--- ").append(fc.getFileName()).append(" ---\n")
                            .append(fc.getContent()).append("\n");
                }
                filePart.append("【用户问题】").append(message);
                fullMessage = filePart.toString();
                
                log.info("使用全文解析: 解析了{}个文件", fileContents.size());
            }
        }

        // 5. 保存用户消息（存原始消息，不带文件内容）
        saveMessage(session.getId(), userId, 1, message, null, null);

        // 6. 调用DeepSeek API（含上下文 + 文件内容）
        long startTime = System.currentTimeMillis();
        // 根据场景获取System Prompt
        String systemPrompt = deepSeekConfig.getSystemPromptByScene(scene);
        String answer = deepSeekService.callDeepSeek(
                deepSeekService.buildMessages(systemPrompt, history, fullMessage));
        long endTime = System.currentTimeMillis();

        // 7. 保存AI回复
        int latency = (int) (endTime - startTime);
        saveMessage(session.getId(), userId, 2, answer, 0, latency);

        // 8. 更新会话状态
        ChatSession update = new ChatSession();
        update.setId(session.getId());
        update.setStatus(1);
        update.setSessionTitle(message.substring(0, Math.min(AiConstants.SESSION_TITLE_MAX_LEN, message.length())));
        sessionMapper.updateChatSession(update);

        // 9. 更新限额
        incrementQuota(userId);

        // 10. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("answer", answer);
        result.put("latency", latency);
        return result;
    }

    @Override
    public List<ChatSession> getHistory(Long userId) {
        ChatSession query = new ChatSession();
        query.setUserId(userId);
        return sessionMapper.selectChatSessionList(query);
    }

    @Override
    public List<ChatMessage> getSessionDetail(Long sessionId, Long userId) {
        // 验证会话归属
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限访问");
        }
        return messageMapper.selectMessagesBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        // 验证会话归属
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限删除");
        }
        // 逻辑删除会话
        sessionMapper.deleteChatSessionById(sessionId);
        // 逻辑删除会话下的所有消息
        messageMapper.deleteChatMessageBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> getUserQuota(Long userId) {
        String key = AiConstants.USER_QUOTA_PREFIX + userId;
        Integer used = redisCache.getCacheObject(key);
        if (used == null) {
            used = 0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("used", used);
        result.put("total", AiConstants.DAILY_QUOTA_LIMIT);
        result.put("remaining", AiConstants.DAILY_QUOTA_LIMIT - used);
        return result;
    }

    /**
     * 检查用户限额
     */
    private void checkQuota(Long userId) {
        String key = AiConstants.USER_QUOTA_PREFIX + userId;
        Integer used = redisCache.getCacheObject(key);
        if (used != null && used >= AiConstants.DAILY_QUOTA_LIMIT) {
            throw new RuntimeException("今日提问次数已用完，请明天再来");
        }
    }

    /**
     * 增加用户限额计数
     */
    private void incrementQuota(Long userId) {
        String key = AiConstants.USER_QUOTA_PREFIX + userId;
        // 使用RedisTemplate直接调用increment
        Long count = redisCache.redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次设置，设置过期时间为当天剩余时间
            long secondsUntilMidnight = Duration.between(LocalDateTime.now(), LocalDateTime.now().with(LocalTime.MAX).withNano(0).plusSeconds(1)).getSeconds();
            redisCache.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        }
    }

    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(Long userId, Long sessionId, String question) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.selectChatSessionById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }

        // 创建新会话
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionTitle(question.substring(0, Math.min(AiConstants.SESSION_TITLE_MAX_LEN, question.length())));
        session.setModelType("deepseek-chat");
        session.setStatus(0);
        session.setTotalTokens(0);
        session.setDeleted(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insertChatSession(session);

        return session;
    }

    /**
     * 保存消息
     */
    private void saveMessage(Long sessionId, Long userId, Integer role, String content,
                             Integer tokensUsed, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setContentShort(content.substring(0, Math.min(AiConstants.CONTENT_SHORT_MAX_LEN, content.length())));
        message.setTokensUsed(tokensUsed);
        message.setLatencyMs(latencyMs);
        message.setIsSaved(0);
        message.setDeleted(0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insertChatMessage(message);
    }
}
