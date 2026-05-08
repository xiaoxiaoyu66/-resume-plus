package com.ruoyi.web.controller.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.ChatSession;

/**
 * 对话会话 Mapper 接口
 */
@Mapper
public interface ChatSessionMapper {

    ChatSession selectSessionById(Long id);

    List<ChatSession> selectSessionList(ChatSession session);

    int insertSession(ChatSession session);

    int updateSession(ChatSession session);

    int deleteSessionById(Long id);

    // 别名方法，用于兼容 Service 调用
    default ChatSession selectChatSessionById(Long id) {
        return selectSessionById(id);
    }

    default List<ChatSession> selectChatSessionList(ChatSession session) {
        return selectSessionList(session);
    }

    default int insertChatSession(ChatSession session) {
        return insertSession(session);
    }

    default int updateChatSession(ChatSession session) {
        return updateSession(session);
    }

    default int deleteChatSessionById(Long id) {
        return deleteSessionById(id);
    }
}
