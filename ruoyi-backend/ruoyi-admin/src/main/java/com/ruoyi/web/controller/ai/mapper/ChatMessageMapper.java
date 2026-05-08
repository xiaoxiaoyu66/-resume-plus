package com.ruoyi.web.controller.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.ChatMessage;

/**
 * 对话消息 Mapper 接口
 */
@Mapper
public interface ChatMessageMapper {

    ChatMessage selectMessageById(Long id);

    List<ChatMessage> selectMessageListBySessionId(Long sessionId);

    int insertMessage(ChatMessage message);

    int deleteMessageBySessionId(Long sessionId);

    ChatMessage getLastMessageBySessionId(Long sessionId);

    // 别名方法，用于兼容 Service 调用
    default List<ChatMessage> selectMessagesBySessionId(Long sessionId) {
        return selectMessageListBySessionId(sessionId);
    }

    default int insertChatMessage(ChatMessage message) {
        return insertMessage(message);
    }

    default int deleteChatMessageBySessionId(Long sessionId) {
        return deleteMessageBySessionId(sessionId);
    }
}
