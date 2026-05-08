package com.ruoyi.web.controller.ai.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 对话消息对象 chat_message
 */
public class ChatMessage {

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 角色（1-用户 2-AI） */
    private Integer role;

    /** 消息内容 */
    private String content;

    /** 内容摘要 */
    private String contentShort;

    /** 消耗token数 */
    private Integer tokensUsed;

    /** 响应延迟（毫秒） */
    private Integer latencyMs;

    /** 是否收藏（0-未收藏 1-已收藏） */
    private Integer isSaved;

    /** 删除标志（0-正常 1-删除） */
    private Integer deleted;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentShort() { return contentShort; }
    public void setContentShort(String contentShort) { this.contentShort = contentShort; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Integer getIsSaved() { return isSaved; }
    public void setIsSaved(Integer isSaved) { this.isSaved = isSaved; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
