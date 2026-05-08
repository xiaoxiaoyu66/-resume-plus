package com.ruoyi.web.controller.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 模块常量配置
 *
 * 集中管理所有 Redis key 前缀、数值常量、缓存 TTL，
 * 消除各 Service 间的重复定义。
 */
@Component
public class AiConstants {

    // ========== Redis Key 前缀 ==========

    /** 用户每日限额计数 */
    public static final String USER_QUOTA_PREFIX = "user:quota:";

    /** 聊天缓存（L2 Redis） */
    public static final String CHAT_CACHE_PREFIX = "chat:cache:";

    /** 聊天相似问题索引 */
    public static final String CHAT_SIMILAR_PREFIX = "chat:similar:";

    /** 滑动窗口上下文 */
    public static final String SESSION_CONTEXT_PREFIX = "chat:context:";

    /** 临时文件 */
    public static final String TEMP_FILE_PREFIX = "temp:file:";

    /** 临时文件索引 */
    public static final String TEMP_FILE_INDEX_PREFIX = "temp:file:index:";

    /** 微信扫码登录会话 */
    public static final String QR_SESSION_PREFIX = "qr_session:";

    /** 微信扫码场景 */
    public static final String QR_SCENE_PREFIX = "qr_scene:";

    /** 短信验证码 */
    public static final String SMS_CODE_PREFIX = "sms_code:";

    /** AI 简历缓存 */
    public static final String AI_RESUME_CACHE_PREFIX = "ai:resume:";

    // ========== 数值常量 ==========

    /** 每日提问次数上限 */
    public static final int DAILY_QUOTA_LIMIT = 50;

    /** SSE 超时（毫秒） */
    public static final long SSE_TIMEOUT_MS = 300_000L;

    /** 文件上传大小限制（字节） */
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /** 文件内容可解析最大大小（MB） */
    public static final int MAX_FILE_SIZE_MB = 5;

    /** 单文件发送给 AI 的最大字符数 */
    public static final int MAX_CHARS_PER_FILE = 3000;

    /** 临时文件过期时间（小时） */
    public static final int TEMP_FILE_EXPIRE_HOURS = 24;

    /** 简历文本解析上限 */
    public static final int RESUME_PARSE_MAX_CHARS = 30000;

    /** 会话标题截断长度 */
    public static final int SESSION_TITLE_MAX_LEN = 50;

    /** 消息内容摘要截断长度 */
    public static final int CONTENT_SHORT_MAX_LEN = 200;

    /** 向量检索 Top-K */
    public static final int VECTOR_SEARCH_TOP_K = 5;

    /** 对话记忆检索 Top-K */
    public static final int MEMORY_SEARCH_TOP_K = 3;

    /** 文本分块大小 */
    public static final int CHUNK_SIZE = 800;

    /** 文本分块重叠 */
    public static final int CHUNK_OVERLAP = 100;

    /** Token 估算比例（每字符） */
    public static final int TOKEN_PER_CHAR = 4;

    // ========== 缓存 TTL（秒） ==========

    /** 简历缓存 TTL（24 小时） */
    public static final long CACHE_TTL_RESUME = 86400L;

    /** 普通 AI 生成缓存 TTL（1 小时） */
    public static final long CACHE_TTL_GENERATE = 3600L;

    /** JD 匹配缓存 TTL（1 小时） */
    public static final long CACHE_TTL_JD = 3600L;

    /** 用户限额计数器 TTL（自动计算到午夜，此为默认值） */
    public static final long QUOTA_DEFAULT_TTL = 86400L;

    // ========== Elasticsearch ==========

    public static final String ES_INDEX_FILE = "file_contents";
}
