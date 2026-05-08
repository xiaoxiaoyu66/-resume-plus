package com.ruoyi.web.controller.ai.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;

/**
 * MinIO 配置类
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Resource
    private MinioProperties minioProperties;

    /**
     * 创建 MinIO 客户端
     */
    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        // 初始化存储桶
        initBucket(client);

        return client;
    }

    /**
     * 初始化存储桶
     */
    private void initBucket(MinioClient client) {
        try {
            String bucketName = minioProperties.getBucketName();
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("MinIO 存储桶 '{}' 创建成功", bucketName);

                // 设置存储桶为公共读（可选，根据需求调整）
                setBucketPolicy(client, bucketName);
            } else {
                log.info("MinIO 存储桶 '{}' 已存在", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO 存储桶初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 设置存储桶策略（允许公共读取）
     */
    private void setBucketPolicy(MinioClient client, String bucketName) {
        try {
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": \"*\",\n" +
                    "            \"Action\": [\n" +
                    "                \"s3:GetObject\"\n" +
                    "            ],\n" +
                    "            \"Resource\": [\n" +
                    "                \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

            client.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            log.info("MinIO 存储桶 '{}' 策略设置成功", bucketName);
        } catch (Exception e) {
            log.error("MinIO 存储桶策略设置失败: {}", e.getMessage(), e);
        }
    }
}
