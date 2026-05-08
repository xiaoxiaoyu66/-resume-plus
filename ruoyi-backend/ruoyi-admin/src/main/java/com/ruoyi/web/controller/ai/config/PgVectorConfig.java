package com.ruoyi.web.controller.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * PGVector 数据源配置
 */
@Configuration
public class PgVectorConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorConfig.class);

    @Resource
    private PgVectorProperties pgVectorProperties;

    /**
     * 创建 PGVector 数据源
     */
    @Bean
    public DataSource pgVectorDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(pgVectorProperties.getJdbcUrl());
        config.setUsername(pgVectorProperties.getUsername());
        config.setPassword(pgVectorProperties.getPassword());
        config.setMaximumPoolSize(pgVectorProperties.getMaxConnections());
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("PgVectorPool");

        HikariDataSource dataSource = new HikariDataSource(config);

        // 初始化扩展
        initPgVectorExtension(dataSource);

        log.info("PGVector 数据源初始化完成: {}", pgVectorProperties.getJdbcUrl());
        return dataSource;
    }

    /**
     * 初始化 pgvector 扩展
     */
    private void initPgVectorExtension(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 启用向量扩展
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // 创建向量表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS document_vectors (
                    id BIGSERIAL PRIMARY KEY,
                    file_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    chunk_index INTEGER NOT NULL,
                    chunk_text TEXT NOT NULL,
                    embedding vector(768),
                    token_count INTEGER,
                    metadata JSONB,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_file_chunk UNIQUE (file_id, chunk_index)
                )
                """);

            // 创建向量索引
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_vector_embedding 
                ON document_vectors 
                USING ivfflat (embedding vector_cosine_ops) 
                WITH (lists = 100)
                """);

            // 创建查询索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_doc_vectors_user ON document_vectors(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_doc_vectors_file ON document_vectors(file_id)");

            // 创建对话记忆向量表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS conversation_vectors (
                    id BIGSERIAL PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    turn_index INTEGER NOT NULL,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    embedding vector(768),
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_conv_vectors_user ON conversation_vectors(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_conv_vectors_session ON conversation_vectors(session_id)");
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_conv_vectors_embedding
                ON conversation_vectors
                USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100)
                """);

            log.info("PGVector 扩展和表初始化完成");

        } catch (Exception e) {
            log.error("PGVector 初始化失败: {}", e.getMessage(), e);
        }
    }
}
