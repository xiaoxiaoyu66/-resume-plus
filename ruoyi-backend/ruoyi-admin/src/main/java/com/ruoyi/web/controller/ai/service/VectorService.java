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
 * Vector 向量服务
 * 提供向量存储、检索、删除等操作
 */
@Service
public class VectorService {

    private static final Logger log = LoggerFactory.getLogger(VectorService.class);

    @Resource
    private DataSource pgVectorDataSource;

    @Resource
    private EmbeddingService embeddingService;

    /**
     * 保存文档向量
     *
     * @param fileId    文件ID
     * @param userId    用户ID
     * @param chunks    文本分块列表
     * @return 保存的向量数量
     */
    public int saveDocumentVectors(Long fileId, Long userId, List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        int savedCount = 0;

        try (Connection conn = pgVectorDataSource.getConnection()) {
            // 启用自动提交
            conn.setAutoCommit(true);

            String sql = "INSERT INTO document_vectors (file_id, user_id, chunk_index, chunk_text, embedding, token_count) " +
                        "VALUES (?, ?, ?, ?, ?::vector, ?) " +
                        "ON CONFLICT (file_id, chunk_index) DO UPDATE SET " +
                        "chunk_text = EXCLUDED.chunk_text, " +
                        "embedding = EXCLUDED.embedding, " +
                        "token_count = EXCLUDED.token_count, " +
                        "create_time = CURRENT_TIMESTAMP";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 批量生成向量（容错：向量化失败时返回空结果）
                List<float[]> embeddings;
                try {
                    embeddings = embeddingService.embedBatch(chunks);
                } catch (Exception e) {
                    log.warn("文档向量化失败: {}", e.getMessage());
                    return 0;
                }

                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    float[] embedding = embeddings.get(i);

                    if (embedding == null) {
                        log.warn("第 {} 块文本向量化失败，跳过", i);
                        continue;
                    }

                    pstmt.setLong(1, fileId);
                    pstmt.setLong(2, userId);
                    pstmt.setInt(3, i);
                    pstmt.setString(4, chunk);

                    // 将 float[] 转换为 PostgreSQL vector 格式
                    String vectorStr = vectorToString(embedding);
                    pstmt.setString(5, vectorStr);

                    // 估算token数量 (简单估算：中文字符数 + 英文单词数)
                    int tokenCount = estimateTokenCount(chunk);
                    pstmt.setInt(6, tokenCount);

                    pstmt.addBatch();
                    savedCount++;

                    // 每10条执行一次批量插入
                    if (i % 10 == 9 || i == chunks.size() - 1) {
                        pstmt.executeBatch();
                        log.info("批量保存向量: {}/{} 块", i + 1, chunks.size());
                    }
                }
            }

            log.info("文档向量保存完成: fileId={}, 共{}块", fileId, savedCount);
            return savedCount;

        } catch (SQLException e) {
            log.error("保存文档向量失败: fileId={}", fileId, e);
            throw new RuntimeException("保存文档向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 相似度搜索
     *
     * @param queryText 查询文本
     * @param userId    用户ID（数据隔离）
     * @param topK      返回Top-K个结果
     * @return 相似文本列表
     */
    public List<VectorSearchResult> searchSimilar(String queryText, Long userId, int topK) {
        List<VectorSearchResult> results = new ArrayList<>();

        // 查询文本转向量（容错：向量化失败时返回空结果，不阻断业务流程）
        float[] queryVector;
        try {
            queryVector = embeddingService.embed(queryText);
        } catch (Exception e) {
            log.warn("查询文本向量化失败: {}", e.getMessage());
            return results;
        }
        if (queryVector == null) {
            log.warn("查询文本向量化返回空");
            return results;
        }

        String sql = "SELECT id, file_id, chunk_index, chunk_text, token_count, " +
                    "1 - (embedding <=> ?::vector) as similarity " +
                    "FROM document_vectors " +
                    "WHERE user_id = ? " +
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
                    VectorSearchResult result = new VectorSearchResult();
                    result.setId(rs.getLong("id"));
                    result.setFileId(rs.getLong("file_id"));
                    result.setChunkIndex(rs.getInt("chunk_index"));
                    result.setChunkText(rs.getString("chunk_text"));
                    result.setTokenCount(rs.getInt("token_count"));
                    result.setSimilarity(rs.getFloat("similarity"));
                    results.add(result);
                }
            }

            log.info("相似度搜索完成: 找到{}条结果", results.size());
            return results;

        } catch (SQLException e) {
            log.error("相似度搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("相似度搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件的向量
     *
     * @param fileId 文件ID
     */
    public void deleteByFileId(Long fileId) {
        String sql = "DELETE FROM document_vectors WHERE file_id = ?";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, fileId);
            int deleted = pstmt.executeUpdate();

            log.info("删除文件向量: fileId={}, 删除{}条", fileId, deleted);

        } catch (SQLException e) {
            log.error("删除文件向量失败: fileId={}", fileId, e);
            throw new RuntimeException("删除文件向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除用户的所有向量
     *
     * @param userId 用户ID
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM document_vectors WHERE user_id = ?";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            int deleted = pstmt.executeUpdate();

            log.info("删除用户向量: userId={}, 删除{}条", userId, deleted);

        } catch (SQLException e) {
            log.error("删除用户向量失败: userId={}", userId, e);
            throw new RuntimeException("删除用户向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件的向量数量
     *
     * @param fileId 文件ID
     * @return 向量数量
     */
    public int getVectorCount(Long fileId) {
        String sql = "SELECT COUNT(*) FROM document_vectors WHERE file_id = ?";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, fileId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error("获取向量数量失败: fileId={}", fileId, e);
        }

        return 0;
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 字符串格式
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 估算Token数量
     * 简单估算：中文字符数 + 英文单词数
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseCount = 0;
        int englishWordCount = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseCount++;
            }
        }

        // 英文单词数（简单按空格分割）
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.matches("[a-zA-Z]+")) {
                englishWordCount++;
            }
        }

        return chineseCount + englishWordCount;
    }

    /**
     * 向量搜索结果类
     */
    public static class VectorSearchResult {
        private Long id;
        private Long fileId;
        private Integer chunkIndex;
        private String chunkText;
        private Integer tokenCount;
        private Float similarity;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }

        public Integer getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

        public String getChunkText() { return chunkText; }
        public void setChunkText(String chunkText) { this.chunkText = chunkText; }

        public Integer getTokenCount() { return tokenCount; }
        public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

        public Float getSimilarity() { return similarity; }
        public void setSimilarity(Float similarity) { this.similarity = similarity; }
    }
}
