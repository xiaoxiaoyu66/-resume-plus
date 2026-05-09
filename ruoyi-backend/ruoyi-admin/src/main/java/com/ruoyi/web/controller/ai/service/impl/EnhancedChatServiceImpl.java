package com.ruoyi.web.controller.ai.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.web.controller.ai.config.AiConstants;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;
import com.ruoyi.web.controller.ai.domain.ChatMessage;
import com.ruoyi.web.controller.ai.domain.ChatSession;
import com.ruoyi.web.controller.ai.domain.Resume;
import com.ruoyi.web.controller.ai.mapper.ChatMessageMapper;
import com.ruoyi.web.controller.ai.mapper.ChatSessionMapper;
import com.ruoyi.web.controller.ai.mapper.ResumeMapper;
import com.ruoyi.web.controller.ai.service.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 增强版 AI 对话服务
 * 整合 SSE + 滑动窗口记忆 + 高频缓存
 */
@Service
@Primary
public class EnhancedChatServiceImpl implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedChatServiceImpl.class);

    @Resource
    private ChatSessionMapper sessionMapper;

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private SseChatService sseChatService;

    @Resource
    private SlidingWindowService slidingWindowService;

    @Resource
    private ChatCacheService chatCacheService;

    @Resource
    private VectorService vectorService;

    @Resource
    private ConversationMemoryService conversationMemoryService;

    @Resource
    private FileContentService fileContentService;

    @Resource
    private RedisCache redisCache;

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Resource
    private ResumeMapper resumeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chat(Long userId, Long sessionId, String message, List<String> fileNames) {
        return chatWithScene(userId, sessionId, message, fileNames, "default");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chatWithScene(Long userId, Long sessionId, String message, 
                                              List<String> fileNames, String scene) {
        // 1. 检查限额
        checkQuota(userId);

        // 2. 创建或获取会话
        ChatSession session = getOrCreateSession(userId, sessionId, message);

        // 3. 【高频缓存】检查是否有缓存答案
        ChatCacheService.CacheEntry cachedEntry = chatCacheService.get(message, scene);
        if (cachedEntry != null) {
            log.info("缓存命中，直接返回: userId={}, sessionId={}", userId, session.getId());
            
            // 保存用户消息
            saveMessage(session.getId(), userId, 1, message, null, null);
            
            // 保存AI回复（缓存的答案）
            String answer = cachedEntry.getAnswer();
            saveMessage(session.getId(), userId, 2, answer, 0, 0);

            // 【P1: 缓存命中也保存对话记忆】
            saveConversationMemory(session.getId(), userId, message, answer);
            
            // 更新会话和限额
            updateSessionAndQuota(session, message, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", session.getId());
            result.put("answer", answer);
            result.put("latency", 0);
            result.put("cached", true);
            return result;
        }

        // 4. 【P1: 对话长期记忆】延迟向量化 + 检索语义相关的历史对话
        conversationMemoryService.lazyEmbedPending(userId);
        List<ConversationMemoryService.ConversationMemory> memories =
                conversationMemoryService.searchRelevant(userId, message, 3);

        // 5. 【滑动窗口记忆】获取短期上下文，并注入长期记忆
        String systemPrompt = deepSeekConfig.getSystemPromptByScene(scene);
        if (!memories.isEmpty()) {
            StringBuilder memoryBlock = new StringBuilder("\n\n[以下是你与用户的历史对话中与当前问题相关的内容]\n");
            for (ConversationMemoryService.ConversationMemory mem : memories) {
                memoryBlock.append("- 用户曾问：").append(mem.getQuestion()).append("\n")
                           .append("  你曾答：").append(mem.getSummary()).append("\n");
            }
            systemPrompt = systemPrompt + memoryBlock.toString();
        }
        List<ChatMessage> history = slidingWindowService.getContextWindow(session.getId(), systemPrompt);

        // 6. 【P0: 全对话 RAG】始终尝试向量检索文档内容
        String fullMessage = buildFullMessage(message, fileNames, userId);

        // 7. 保存用户消息
        saveMessage(session.getId(), userId, 1, message, null, null);

        // 7. 调用 DeepSeek API（非流式，兼容旧接口）
        long startTime = System.currentTimeMillis();
        JSONArray messages = deepSeekService.buildMessages(systemPrompt, history, fullMessage);
        String answer = deepSeekService.callDeepSeek(messages);
        long endTime = System.currentTimeMillis();
        int latency = (int) (endTime - startTime);

        // 8. 保存AI回复
        saveMessage(session.getId(), userId, 2, answer, 0, latency);

        // 【P1: 保存对话记忆】
        saveConversationMemory(session.getId(), userId, message, answer);

        // 9. 【高频缓存】存入缓存
        chatCacheService.put(message, answer, scene);

        // 10. 更新会话和限额
        updateSessionAndQuota(session, message, userId);

        // 11. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("answer", answer);
        result.put("latency", latency);
        result.put("cached", false);
        return result;
    }

    /**
     * 【SSE 流式对话】核心方法
     * 支持打字机效果和实时输出
     */
    public SseEmitter streamChat(Long userId, Long sessionId, String message,
                                  List<String> fileNames, String scene) {
        return streamChat(userId, sessionId, message, fileNames, scene, null);
    }

    public SseEmitter streamChat(Long userId, Long sessionId, String message,
                                  List<String> fileNames, String scene,
                                  String resumeContext) {
        // 检查限额
        checkQuota(userId);

        // 创建或获取会话
        ChatSession session = getOrCreateSession(userId, sessionId, message);
        final Long finalSessionId = session.getId();

        // 使用会话中保存的 scene（优先），否则用参数
        String effectiveScene = session.getScene();
        if (effectiveScene == null || effectiveScene.isEmpty()) {
            effectiveScene = (scene != null) ? scene : "default";
        }

        // 如果 scene 参数与 session 中不同，保存到 session
        if (scene != null && !scene.equals(effectiveScene) && !"default".equals(scene)) {
            session.setScene(scene);
            sessionMapper.updateChatSession(session);
            effectiveScene = scene;
        }

        String finalScene = effectiveScene; // 用于 lambda 的 effectively final 变量

        // 创建 SSE Emitter（超时5分钟）
        SseEmitter emitter = new SseEmitter(AiConstants.SSE_TIMEOUT_MS);

        // 处理文件内容
        String fullMessage = buildFullMessage(message, fileNames, userId);

        // 保存用户消息
        saveMessage(finalSessionId, userId, 1, message, null, null);

        // 【P1: 对话长期记忆】延迟向量化 + 检索历史对话
        conversationMemoryService.lazyEmbedPending(userId);
        List<ConversationMemoryService.ConversationMemory> memories =
                conversationMemoryService.searchRelevant(userId, message, 3);

        // 【滑动窗口记忆】获取上下文，并注入长期记忆
        String systemPrompt = deepSeekConfig.getSystemPromptByScene(effectiveScene);

        // 简历上下文注入（interview/resume 场景优先）
        String effectiveResumeContext = resumeContext;
        if (effectiveResumeContext == null && session.getResumeId() != null) {
            effectiveResumeContext = buildResumeContext(session.getResumeId());
        }
        if (effectiveResumeContext != null && !effectiveResumeContext.isEmpty()) {
            if ("interview-hr".equals(effectiveScene)) {
                systemPrompt = systemPrompt + "\n\n## 用户简历信息\n" + effectiveResumeContext
                        + "\n\n根据以上简历信息进行HR行为面试。关注求职动机、稳定性、职业规划、软素质。";
            } else if ("interview-pro".equals(effectiveScene)) {
                systemPrompt = systemPrompt + "\n\n## 用户简历信息\n" + effectiveResumeContext
                        + "\n\n根据以上简历信息进行专业面试。注意用户的期望岗位决定出题方向。";
            } else if ("resume".equals(effectiveScene)) {
                systemPrompt = systemPrompt + "\n\n## 用户简历信息\n" + effectiveResumeContext
                        + "\n\n请基于上述简历信息进行诊断分析，从排版、内容、技能匹配度等维度给出评分和改进建议。";
            } else if ("career".equals(effectiveScene)) {
                systemPrompt = systemPrompt + "\n\n## 用户简历信息\n" + effectiveResumeContext
                        + "\n\n请基于上述简历信息，结合用户的技能和经历，提供职业发展规划建议。";
            }
        }

        if (!memories.isEmpty()) {
            StringBuilder memoryBlock = new StringBuilder("\n\n[以下是你与用户的历史对话中与当前问题相关的内容]\n");
            for (ConversationMemoryService.ConversationMemory mem : memories) {
                memoryBlock.append("- 用户曾问：").append(mem.getQuestion()).append("\n")
                           .append("  你曾答：").append(mem.getSummary()).append("\n");
            }
            systemPrompt = systemPrompt + memoryBlock.toString();
        }
        List<ChatMessage> history = slidingWindowService.getContextWindow(finalSessionId, systemPrompt);
        JSONArray messages = deepSeekService.buildMessages(systemPrompt, history, fullMessage);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 【SSE】流式调用
        sseChatService.streamChat(messages, emitter,
            // 完成回调
            fullAnswer -> {
                long latency = (int) (System.currentTimeMillis() - startTime);
                
                // 保存AI回复
                saveMessage(finalSessionId, userId, 2, fullAnswer, 0, (int) latency);
                
                // 【P1: 保存对话记忆】
                saveConversationMemory(finalSessionId, userId, message, fullAnswer);

                // 【高频缓存】存入缓存
                chatCacheService.put(message, fullAnswer, finalScene);
                
                // 更新会话和限额
                updateSessionAndQuota(session, message, userId);
                
                log.info("SSE对话完成: sessionId={}, latency={}ms", finalSessionId, latency);
            },
            // 错误回调
            errorMsg -> {
                log.error("SSE对话失败: sessionId={}, error={}", finalSessionId, errorMsg);
            },
            // 会话ID
            finalSessionId
        );

        // 处理连接关闭
        emitter.onCompletion(() -> log.debug("SSE连接完成: sessionId={}", finalSessionId));
        emitter.onTimeout(() -> log.warn("SSE连接超时: sessionId={}", finalSessionId));
        emitter.onError(e -> log.error("SSE连接错误: sessionId={}", finalSessionId, e));

        return emitter;
    }

    /**
     * 构建完整消息（P0: 始终尝试文档RAG + 文件内容）
     */
    private String buildFullMessage(String message, List<String> fileNames, Long userId) {
        // P0: 始终尝试向量检索文档内容（不依赖是否有 fileNames）
        List<VectorService.VectorSearchResult> searchResults = vectorService.searchSimilar(message, userId, 5);

        if (!searchResults.isEmpty()) {
            StringBuilder contextBuilder = new StringBuilder(
                    "\n\n请参考以下检索到的相关内容回答用户问题。注意：请基于这些内容回答，但不要复述「相关内容1」之类的标记，直接用自然语言组织回答。如果检索内容与问题无关，忽略并用自己的知识回答。\n");
            for (int i = 0; i < searchResults.size(); i++) {
                VectorService.VectorSearchResult result = searchResults.get(i);
                contextBuilder.append("--- 相关内容 ").append(i + 1).append(" (相似度: ")
                        .append(String.format("%.2f", result.getSimilarity())).append(") ---\n")
                        .append(result.getChunkText()).append("\n\n");
            }
            contextBuilder.append("【用户问题】").append(message);
            return contextBuilder.toString();
        }

        // 如果指定了具体文件，回退到全文解析
        if (fileNames != null && !fileNames.isEmpty()) {
            List<FileContentService.FileContent> fileContents = fileContentService.parseFiles(fileNames);
            StringBuilder filePart = new StringBuilder("\n\n用户上传了以下文件，请基于文件内容回答用户的问题。如果文件中找不到相关信息，请如实告知。\n");
            for (FileContentService.FileContent fc : fileContents) {
                filePart.append("--- ").append(fc.getFileName()).append(" ---\n")
                        .append(fc.getContent()).append("\n");
            }
            filePart.append("【用户问题】").append(message);
            return filePart.toString();
        }

        return message;
    }

    /**
     * 更新会话和限额
     */
    private void updateSessionAndQuota(ChatSession session, String message, Long userId) {
        // 更新会话状态
        ChatSession update = new ChatSession();
        update.setId(session.getId());
        update.setStatus(1);
        update.setSessionTitle(message.substring(0, Math.min(AiConstants.SESSION_TITLE_MAX_LEN, message.length())));
        sessionMapper.updateChatSession(update);

        // 更新限额
        incrementQuota(userId);
    }

    @Override
    public List<ChatSession> getHistory(Long userId) {
        ChatSession query = new ChatSession();
        query.setUserId(userId);
        return sessionMapper.selectChatSessionList(query);
    }

    @Override
    public List<ChatMessage> getSessionDetail(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限访问");
        }
        return messageMapper.selectMessagesBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限删除");
        }
        sessionMapper.deleteChatSessionById(sessionId);
        messageMapper.deleteChatMessageBySessionId(sessionId);
        
        // 清除上下文缓存
        slidingWindowService.clearContext(sessionId);
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
     * 获取缓存统计
     */
    public ChatCacheService.CacheStats getCacheStats() {
        return chatCacheService.getStats();
    }

    private void checkQuota(Long userId) {
        String key = AiConstants.USER_QUOTA_PREFIX + userId;
        Integer used = redisCache.getCacheObject(key);
        if (used != null && used >= AiConstants.DAILY_QUOTA_LIMIT) {
            throw new RuntimeException("今日提问次数已用完，请明天再来");
        }
    }

    private void incrementQuota(Long userId) {
        String key = AiConstants.USER_QUOTA_PREFIX + userId;
        Long count = redisCache.redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            long secondsUntilMidnight = Duration.between(
                    LocalDateTime.now(), 
                    LocalDateTime.now().with(LocalTime.MAX).withNano(0).plusSeconds(1)
            ).getSeconds();
            redisCache.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        }
    }

    private ChatSession getOrCreateSession(Long userId, Long sessionId, String question) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.selectChatSessionById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionTitle(question.substring(0, Math.min(AiConstants.SESSION_TITLE_MAX_LEN, question.length())));
        session.setModelType("deepseek-chat");
        session.setScene("default");
        session.setStatus(0);
        session.setTotalTokens(0);
        session.setDeleted(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        // 自动绑定用户最新简历
        try {
            List<Resume> resumes = resumeMapper.selectResumeListByUserId(userId);
            if (resumes != null && !resumes.isEmpty()) {
                session.setResumeId(resumes.get(0).getId());
            }
        } catch (Exception e) {
            log.warn("自动绑定简历失败（不影响主流程）", e);
        }

        sessionMapper.insertChatSession(session);

        return session;
    }

    /**
     * P1: 保存一轮对话到长期记忆（embedding 延迟生成）
     */
    private void saveConversationMemory(Long sessionId, Long userId, String question, String answer) {
        try {
            int msgCount = messageMapper.selectMessagesBySessionId(sessionId).size();
            int turnIndex = msgCount / 2; // 每轮=用户+AI两条
            conversationMemoryService.saveMemory(sessionId, userId, turnIndex, question, answer);
        } catch (Exception e) {
            log.warn("保存对话记忆失败（不影响主流程）: sessionId={}", sessionId, e);
        }
    }

    /**
     * 从简历ID构建 resumeContext 文本（纯文本，不含 HTML）
     */
    private String buildResumeContext(Long resumeId) {
        try {
            Resume resume = resumeMapper.selectResumeById(resumeId);
            if (resume == null || resume.getContent() == null) return null;
            return resume.getContent();
        } catch (Exception e) {
            log.warn("构建简历上下文失败", e);
            return null;
        }
    }

    /**
     * 获取会话（含权限校验）
     */
    public ChatSession getSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return null;
        }
        return session;
    }

    /**
     * 更新会话场景
     */
    public void updateSessionScene(Long sessionId, Long userId, String scene) {
        ChatSession session = sessionMapper.selectChatSessionById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限");
        }
        session.setScene(scene);
        sessionMapper.updateChatSession(session);
    }

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
