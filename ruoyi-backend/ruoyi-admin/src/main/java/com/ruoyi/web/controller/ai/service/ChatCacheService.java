package com.ruoyi.web.controller.ai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.web.controller.ai.config.AiConstants;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 高频缓存服务
 * 多级缓存架构：L1 Caffeine本地缓存 + L2 Redis分布式缓存
 */
@Service
public class ChatCacheService {

    private static final Logger log = LoggerFactory.getLogger(ChatCacheService.class);

    @Resource
    private RedisCache redisCache;

    @Value("${ai.cache.l1-size:1000}")
    private int l1MaxSize; // L1缓存最大条目数

    @Value("${ai.cache.l1-ttl:10}")
    private int l1TtlMinutes; // L1缓存过期时间（分钟）

    @Value("${ai.cache.l2-ttl:60}")
    private int l2TtlMinutes; // L2缓存过期时间（分钟）

    @Value("${ai.cache.similarity-threshold:0.85}")
    private double similarityThreshold; // 语义相似度阈值

    // L1: Caffeine 本地内存缓存（对话）
    private Cache<String, CacheEntry> l1Cache;

    // L1: Caffeine 本地内存缓存（简历 AI 操作）
    private Cache<String, String> resumeL1Cache;

    @PostConstruct
    public void init() {
        l1Cache = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(l1TtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
        resumeL1Cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("高频缓存服务初始化完成: L1大小={}, L1过期={}分钟, L2过期={}分钟",
                l1MaxSize, l1TtlMinutes, l2TtlMinutes);
    }

    /**
     * 缓存条目
     */
    public static class CacheEntry {
        private final String question;
        private final String answer;
        private final long timestamp;
        private final String scene;

        public CacheEntry(String question, String answer, String scene) {
            this.question = question;
            this.answer = answer;
            this.scene = scene;
            this.timestamp = System.currentTimeMillis();
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getScene() {
            return scene;
        }
    }

    /**
     * 获取缓存答案
     *
     * @param question 用户问题
     * @param scene 场景
     * @return 缓存的答案，未命中返回null
     */
    public CacheEntry get(String question, String scene) {
        String cacheKey = generateCacheKey(question, scene);

        // 1. 查询 L1 缓存
        CacheEntry entry = l1Cache.getIfPresent(cacheKey);
        if (entry != null) {
            log.debug("L1缓存命中: question={}", truncate(question, 30));
            return entry;
        }

        // 2. 查询 L2 缓存 (Redis)
        String l2Key = AiConstants.CHAT_CACHE_PREFIX + cacheKey;
        entry = redisCache.getCacheObject(l2Key);
        if (entry != null) {
            log.debug("L2缓存命中: question={}", truncate(question, 30));
            // 回填 L1 缓存
            l1Cache.put(cacheKey, entry);
            return entry;
        }

        // 3. 语义相似匹配（简化版：使用问题哈希前缀匹配）
        entry = findSimilarAnswer(question, scene);
        if (entry != null) {
            log.debug("相似问题缓存命中: question={}", truncate(question, 30));
            return entry;
        }

        log.debug("缓存未命中: question={}", truncate(question, 30));
        return null;
    }

    /**
     * 存入缓存
     *
     * @param question 问题
     * @param answer 答案
     * @param scene 场景
     */
    public void put(String question, String answer, String scene) {
        String cacheKey = generateCacheKey(question, scene);
        CacheEntry entry = new CacheEntry(question, answer, scene);

        // 存入 L1
        l1Cache.put(cacheKey, entry);

        // 存入 L2
        String l2Key = AiConstants.CHAT_CACHE_PREFIX + cacheKey;
        redisCache.setCacheObject(l2Key, entry, l2TtlMinutes, TimeUnit.MINUTES);

        // 存入相似问题索引（用于后续相似匹配）
        String similarKey = generateSimilarityKey(question);
        redisCache.setCacheObject(AiConstants.CHAT_SIMILAR_PREFIX + similarKey, cacheKey, l2TtlMinutes, TimeUnit.MINUTES);

        log.debug("缓存已更新: question={}", truncate(question, 30));
    }

    /**
     * 查找相似问题的答案
     * 简化实现：使用问题关键词匹配
     */
    private CacheEntry findSimilarAnswer(String question, String scene) {
        // 提取关键词（简单实现：取前10个字符作为关键词）
        String keyword = extractKeyword(question);
        if (keyword.isEmpty()) {
            return null;
        }

        // 这里可以实现更复杂的语义相似度计算
        // 目前使用简化策略：检查是否有相似关键词的缓存
        String similarKey = generateSimilarityKey(keyword);
        String cacheKey = redisCache.getCacheObject(AiConstants.CHAT_SIMILAR_PREFIX + similarKey);

        if (cacheKey != null) {
            // 找到相似问题，返回其答案
            CacheEntry entry = l1Cache.getIfPresent(cacheKey);
            if (entry == null) {
                entry = redisCache.getCacheObject(AiConstants.CHAT_CACHE_PREFIX + cacheKey);
            }
            return entry;
        }

        return null;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String question, String scene) {
        String content = scene + ":" + question.trim().toLowerCase();
        return md5Hash(content);
    }

    /**
     * 生成相似度索引键
     */
    private String generateSimilarityKey(String question) {
        String keyword = extractKeyword(question);
        return md5Hash(keyword);
    }

    /**
     * 提取关键词
     */
    private String extractKeyword(String question) {
        // 简单实现：去除标点，取前15个字符
        String cleaned = question.replaceAll("[\\p{P}\\s]", "").toLowerCase();
        return cleaned.length() > 15 ? cleaned.substring(0, 15) : cleaned;
    }

    /**
     * MD5 哈希
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 降级：使用字符串哈希
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 使缓存失效
     *
     * @param question 问题
     * @param scene 场景
     */
    public void invalidate(String question, String scene) {
        String cacheKey = generateCacheKey(question, scene);
        l1Cache.invalidate(cacheKey);
        redisCache.deleteObject(AiConstants.CHAT_CACHE_PREFIX + cacheKey);
        log.debug("缓存已失效: question={}", truncate(question, 30));
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        l1Cache.invalidateAll();
        // 注意：这里不清空Redis，因为可能包含其他数据
        log.info("L1缓存已清空");
    }

    /**
     * 获取缓存的 AI 响应（简历分析、JD匹配等非对话场景）
     * 两级缓存：L1 Caffeine → L2 Redis
     *
     * @param action  操作类型（polish, jd, clinic, generate, star, optimize）
     * @param input   输入内容（用于生成缓存键）
     * @return 缓存的 JSON 响应字符串，未命中返回 null
     */
    public String getAiCache(String action, String input) {
        String cacheKey = action + ":" + md5Hash(input);

        // 1. L1 查询
        String l1Result = resumeL1Cache.getIfPresent(cacheKey);
        if (l1Result != null) {
            log.debug("AI缓存(L1)命中: action={}", action);
            return l1Result;
        }

        // 2. L2 查询 (Redis)
        String redisKey = AiConstants.AI_RESUME_CACHE_PREFIX + cacheKey;
        String l2Result = redisCache.getCacheObject(redisKey);
        if (l2Result != null) {
            log.debug("AI缓存(L2)命中: action={}", action);
            resumeL1Cache.put(cacheKey, l2Result); // 回填 L1
            return l2Result;
        }

        return null;
    }

    /**
     * 缓存 AI 响应
     * 同时写入 L1 和 L2
     *
     * @param action   操作类型
     * @param input    输入内容
     * @param response AI 响应的 JSON 字符串
     */
    public void putAiCache(String action, String input, String response) {
        String cacheKey = action + ":" + md5Hash(input);

        // L1
        resumeL1Cache.put(cacheKey, response);

        // L2
        String redisKey = AiConstants.AI_RESUME_CACHE_PREFIX + cacheKey;
        redisCache.setCacheObject(redisKey, response, (int) AiConstants.CACHE_TTL_RESUME, TimeUnit.SECONDS);
        log.debug("AI缓存已更新: action={}", action);
    }

    /**
     * 使 AI 缓存失效（同时清除 L1 + L2）
     */
    public void invalidateAiCache(String action, String input) {
        String cacheKey = action + ":" + md5Hash(input);
        resumeL1Cache.invalidate(cacheKey);
        String redisKey = AiConstants.AI_RESUME_CACHE_PREFIX + cacheKey;
        redisCache.deleteObject(redisKey);
        log.debug("AI缓存已失效: action={}", action);
    }

    /**
     * 获取缓存统计
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = l1Cache.stats();
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate(),
                l1Cache.estimatedSize()
        );
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long size;

        public CacheStats(long hitCount, long missCount, double hitRate, long size) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.size = size;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public double getHitRate() {
            return hitRate;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
