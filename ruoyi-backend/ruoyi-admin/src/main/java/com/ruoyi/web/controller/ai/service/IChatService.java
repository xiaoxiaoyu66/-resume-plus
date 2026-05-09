package com.ruoyi.web.controller.ai.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.web.controller.ai.domain.ChatSession;
import com.ruoyi.web.controller.ai.domain.ChatMessage;

/**
 * AI 聊天服务接口
 */
public interface IChatService {

    /**
     * 发送对话消息（非流式）
     *
     * @param userId    用户ID
     * @param sessionId 会话ID（可为null，此时创建新会话）
     * @param message   用户消息
     * @param fileNames 关联的文件名列表（MinIO 路径）
     * @return AI 回复内容
     */
    Map<String, Object> chat(Long userId, Long sessionId, String message, List<String> fileNames);

    /**
     * 发送对话消息（指定场景）
     *
     * @param userId    用户ID
     * @param sessionId 会话ID（可为null，此时创建新会话）
     * @param message   用户消息
     * @param fileNames 关联的文件名列表（MinIO 路径）
     * @param scene     场景：default/resume/interview-hr/interview-pro/career
     * @return AI 回复内容
     */
    Map<String, Object> chatWithScene(Long userId, Long sessionId, String message, List<String> fileNames, String scene);

    /**
     * 获取用户历史会话列表
     */
    List<ChatSession> getHistory(Long userId);

    /**
     * 获取会话详情（消息列表）
     */
    List<ChatMessage> getSessionDetail(Long sessionId, Long userId);

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 获取用户额度信息
     *
     * @param userId 用户ID
     * @return 额度信息 {used: 已使用次数, total: 总额度, remaining: 剩余次数}
     */
    Map<String, Object> getUserQuota(Long userId);
}
