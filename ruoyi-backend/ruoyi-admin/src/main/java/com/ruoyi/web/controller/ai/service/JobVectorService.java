package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.domain.JobPosting;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位向量化匹配服务（Layer 3）
 * 将岗位描述转为向量存入 PGVector，通过余弦相似度计算匹配
 */
@Service
public class JobVectorService {

    private static final Logger log = LoggerFactory.getLogger(JobVectorService.class);

    @Resource
    private DataSource pgVectorDataSource;

    @Resource
    private EmbeddingService embeddingService;

    @Lazy
    @Resource
    private JobPostingService jobPostingService;

    /**
     * 将岗位批量嵌入向量（用于初始化/定时任务）
     */
    public int embedAllJobs() {
        JobPosting query = new JobPosting();
        query.setDeleted(0);
        List<JobPosting> jobs = jobPostingService.selectJobList(query);
        if (jobs.isEmpty()) return 0;

        int count = 0;
        for (JobPosting job : jobs) {
            try {
                embedSingleJob(job);
                count++;
            } catch (Exception e) {
                log.warn("岗位向量化失败: jobId={}, {}", job.getId(), e.getMessage());
            }
        }
        log.info("批量向量化完成: {}/{}", count, jobs.size());
        return count;
    }

    /**
     * 嵌入单条岗位并存入 PGVector
     */
    public void embedSingleJob(JobPosting job) {
        if (job.getId() == null) return;

        // 构建嵌入文本
        String text = buildEmbeddingText(job);

        // 向量化
        float[] vector;
        try {
            vector = embeddingService.embed(text);
        } catch (Exception e) {
            log.warn("向量化失败: jobId={}", job.getId());
            return;
        }
        if (vector == null) return;

        // 写入 job_embedding 表
        ensureTable();
        String sql = "INSERT INTO job_embedding (job_id, embedding, updated_at) VALUES (?, ?::vector, CURRENT_TIMESTAMP) " +
                    "ON CONFLICT (job_id) DO UPDATE SET embedding = EXCLUDED.embedding, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, job.getId());
            pstmt.setString(2, vectorToString(vector));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("嵌入向量写入失败: jobId={}", job.getId(), e);
        }
    }

    /**
     * 计算用户画像与所有岗位的向量相似度
     *
     * @param profileText 用户画像文本（skills + selfIntro + projects）
     * @return jobId → 相似度(0~1) 映射
     */
    public Map<Long, Float> computeScores(String profileText) {
        Map<Long, Float> result = new LinkedHashMap<>();
        if (profileText == null || profileText.trim().isEmpty()) return result;

        // 1. 画像向量化
        float[] queryVector;
        try {
            queryVector = embeddingService.embed(profileText);
        } catch (Exception e) {
            log.warn("画像向量化失败: {}", e.getMessage());
            return result;
        }
        if (queryVector == null) return result;

        // 2. 查询全部岗位向量，计算余弦相似度
        ensureTable();
        String sql = "SELECT job_id, 1 - (embedding <=> ?::vector) AS similarity FROM job_embedding ORDER BY similarity DESC";

        try (Connection conn = pgVectorDataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vectorToString(queryVector));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getLong("job_id"), rs.getFloat("similarity"));
                }
            }
        } catch (SQLException e) {
            log.error("向量相似度查询失败", e);
        }

        log.info("向量匹配完成: 共{}条", result.size());
        return result;
    }

    /**
     * 构建岗位的嵌入文本
     */
    private String buildEmbeddingText(JobPosting job) {
        StringBuilder sb = new StringBuilder();
        sb.append("职位：").append(job.getTitle() != null ? job.getTitle() : "").append("\n");
        sb.append("公司：").append(job.getCompany() != null ? job.getCompany() : "").append("\n");

        if (job.getTags() != null && !job.getTags().isEmpty()) {
            sb.append("技能要求：").append(job.getTags()).append("\n");
        }
        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            sb.append("岗位职责：").append(job.getDescription()).append("\n");
        }
        if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
            sb.append("招聘要求：").append(job.getRequirements()).append("\n");
        }
        sb.append("地点：").append(job.getLocation() != null ? job.getLocation() : "").append("\n");
        sb.append("学历要求：").append(job.getEducation() != null ? job.getEducation() : "").append("\n");
        sb.append("经验要求：").append(job.getExperience() != null ? job.getExperience() : "").append("\n");

        return sb.toString();
    }

    /**
     * 确保 job_embedding 表存在
     */
    private void ensureTable() {
        try (Connection conn = pgVectorDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS job_embedding (" +
                        "job_id BIGINT NOT NULL PRIMARY KEY, " +
                        "embedding vector(768) NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            log.error("确保 job_embedding 表存在失败", e);
        }
    }

    /**
     * float[] → PostgreSQL vector 字符串
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
}
