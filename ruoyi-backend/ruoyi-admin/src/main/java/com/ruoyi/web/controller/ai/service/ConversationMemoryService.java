package com.ruoyi.web.controller.ai.service;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话记忆向量服务
 *
 * 长期记忆：每轮对话保存到 conversation_vectors 表，
 * embedding 延迟生成（用户发下一条消息时再处理），
 * 新对话时通过向量检索召回语义相关的历史对话。
 */
@Service
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    @Resource
    private DataSource pgVectorDataSource;

    @Resource
    private EmbeddingService embeddingService;

    /**
     * 保存一轮对话（不含 embedding，延迟生成）
     */
    public void saveMemory(Long sessionId, Long userId, int turnIndex, String question, String answer) {
        String summary = buildSummary(question, answer);

        String sql = "INSERT INTO conversation_vectors " +
                     "(session_id, user_id, turn_index, question, answer, summary) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, sessionId);
            pstmt.setLong(2, userId);
            pstmt.setInt(3, turnIndex);
            pstmt.setString(4, truncate(question, 500));
            pstmt.setString(5, truncate(answer, 1000));
            pstmt.setString(6, summary);
            pstmt.executeUpdate();

            log.debug("对话记忆已保存: sessionId={}, turn={}", sessionId, turnIndex);

        } catch (SQLException e) {
            log.error("保存对话记忆失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 延迟生成 pending 的 embedding
     * 查找当前用户所有 embedding 为 NULL 的记录，批量生成并更新
     */
    public void lazyEmbedPending(Long userId) {
        List<PendingMemory> pendings = findPendingMemories(userId);
        if (pendings.isEmpty()) {
            return;
        }

        log.info("发现 {} 条未向量化的对话记忆，开始批量生成", pendings.size());

        List<String> texts = pendings.stream()
                .map(m -> m.summary)
                .toList();

        List<float[]> embeddings;
        try {
            embeddings = embeddingService.embedBatch(texts);
        } catch (Exception e) {
            log.warn("对话记忆向量化失败: {}", e.getMessage());
            return;
        }

        if (embeddings.size() != pendings.size()) {
            log.warn("向量化结果数量不匹配: expected={}, actual={}", pendings.size(), embeddings.size());
            return;
        }

        String updateSql = "UPDATE conversation_vectors SET embedding = ?::vector WHERE id = ?";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            for (int i = 0; i < pendings.size(); i++) {
                PendingMemory m = pendings.get(i);
                float[] embedding = embeddings.get(i);
                if (embedding == null) continue;

                pstmt.setString(1, vectorToString(embedding));
                pstmt.setLong(2, m.id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();

            log.info("对话记忆向量化完成: {} 条", pendings.size());

        } catch (SQLException e) {
            log.error("更新对话记忆向量失败", e);
        }
    }

    /**
     * 搜索与当前问题相关的历史对话
     */
    public List<ConversationMemory> searchRelevant(Long userId, String questionText, int topK) {
        List<ConversationMemory> results = new ArrayList<>();

        // 先检查是否有已向量化的记忆
        if (countEmbedded(userId) == 0) {
            return results;
        }

        // 问题转向量
        float[] queryVector;
        try {
            queryVector = embeddingService.embed(questionText);
        } catch (Exception e) {
            log.warn("问题向量化失败: {}", e.getMessage());
            return results;
        }
        if (queryVector == null) return results;

        String sql = "SELECT id, session_id, turn_index, question, answer, summary, " +
                     "1 - (embedding <=> ?::vector) AS similarity " +
                     "FROM conversation_vectors " +
                     "WHERE user_id = ? AND embedding IS NOT NULL " +
                     "ORDER BY embedding <=> ?::vector " +
                     "LIMIT ?";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String vectorStr = vectorToString(queryVector);
            pstmt.setString(1, vectorStr);
            pstmt.setLong(2, userId);
            pstmt.setString(3, vectorStr);
            pstmt.setInt(4, topK);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ConversationMemory mem = new ConversationMemory();
                    mem.setId(rs.getLong("id"));
                    mem.setSessionId(rs.getLong("session_id"));
                    mem.setTurnIndex(rs.getInt("turn_index"));
                    mem.setQuestion(rs.getString("question"));
                    mem.setAnswer(rs.getString("answer"));
                    mem.setSummary(rs.getString("summary"));
                    mem.setSimilarity(rs.getFloat("similarity"));
                    results.add(mem);
                }
            }

            log.debug("对话记忆检索完成: userId={}, 找到{}条", userId, results.size());

        } catch (SQLException e) {
            log.error("对话记忆检索失败", e);
        }

        return results;
    }

    /**
     * 统计用户已向量化的记忆数
     */
    public int countEmbedded(Long userId) {
        String sql = "SELECT COUNT(*) FROM conversation_vectors WHERE user_id = ? AND embedding IS NOT NULL";
        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("统计对话记忆失败", e);
        }
        return 0;
    }

    /**
     * 统计用户所有记忆数（含未向量化）
     */
    public int countTotal(Long userId) {
        String sql = "SELECT COUNT(*) FROM conversation_vectors WHERE user_id = ?";
        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("统计对话记忆失败", e);
        }
        return 0;
    }

    // ========== 内部方法 ==========

    private List<PendingMemory> findPendingMemories(Long userId) {
        List<PendingMemory> pendings = new ArrayList<>();
        String sql = "SELECT id, session_id, summary FROM conversation_vectors " +
                     "WHERE user_id = ? AND embedding IS NULL ORDER BY create_time ASC LIMIT 20";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PendingMemory m = new PendingMemory();
                    m.id = rs.getLong("id");
                    m.sessionId = rs.getLong("session_id");
                    m.summary = rs.getString("summary");
                    pendings.add(m);
                }
            }
        } catch (SQLException e) {
            log.error("查询待向量化记忆失败", e);
        }
        return pendings;
    }

    /**
     * 生成摘要文本（用于 embedding 检索）
     * 格式：问题 + 答案要点
     */
    private String buildSummary(String question, String answer) {
        String q = question != null ? question.trim() : "";
        String a = answer != null ? answer.trim() : "";
        // 取答案前 200 字作为摘要
        String aShort = a.length() > 200 ? a.substring(0, 200) : a;
        return "问：" + q + " 答：" + aShort;
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncate(String str, int maxLen) {
        if (str == null || str.length() <= maxLen) return str;
        return str.substring(0, maxLen);
    }

    // ========== 内部类 ==========

    private static class PendingMemory {
        long id;
        long sessionId;
        String summary;
    }

    /**
     * 对话记忆检索结果
     */
    public static class ConversationMemory {
        private Long id;
        private Long sessionId;
        private Integer turnIndex;
        private String question;
        private String answer;
        private String summary;
        private Float similarity;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

        public Integer getTurnIndex() { return turnIndex; }
        public void setTurnIndex(Integer turnIndex) { this.turnIndex = turnIndex; }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public Float getSimilarity() { return similarity; }
        public void setSimilarity(Float similarity) { this.similarity = similarity; }
    }
}
