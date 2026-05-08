package com.ruoyi.web.controller.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elasticsearch 配置测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class ElasticsearchConfigTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Test
    void testElasticsearchClientNotNull() {
        assertNotNull(elasticsearchClient, "ElasticsearchClient 应该被正确创建");
    }

    @Test
    void testElasticsearchConnection() throws IOException {
        // 测试连接
        InfoResponse info = elasticsearchClient.info();
        assertNotNull(info, "应该能获取到 ES 集群信息");
        assertNotNull(info.clusterName(), "集群名称不应该为空");
        assertNotNull(info.version(), "版本信息不应该为空");
        
        System.out.println("ES 集群名称: " + info.clusterName());
        System.out.println("ES 版本: " + info.version().number());
    }
}
