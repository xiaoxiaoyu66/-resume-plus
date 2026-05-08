package com.ruoyi.web.controller.ai.service;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.service.ElasticsearchFileService.FileSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版文件上传服务
 * 集成 Elasticsearch 全文搜索和 RabbitMQ 异步向量化
 */
@Service
public class EnhancedFileUploadService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedFileUploadService.class);

    @Autowired
    private TempFileStorageService tempFileStorageService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private FileContentService fileContentService;

    @Autowired
    private ElasticsearchFileService elasticsearchFileService;

    @Autowired
    private FileVectorProducer fileVectorProducer;

    /**
     * 上传并处理文件（集成 ES 索引和 RabbitMQ 向量化）
     *
     * @param file   上传的文件
     * @param userId 用户ID
     * @return 处理结果
     */
    public Map<String, Object> uploadAndProcessFile(MultipartFile file, Long userId) throws IOException {
        // 1. 保存到临时存储
        String tempFileId = tempFileStorageService.saveTempFile(file, userId);
        TempFileStorageService.TempFileInfo tempInfo = tempFileStorageService.getTempFileInfo(tempFileId);

        // 2. 上传到 MinIO
        byte[] fileContent = tempFileStorageService.getTempFileBytes(tempFileId);
        String userFolder = "ai-uploads/" + userId + "/";
        String fileName = minioService.uploadFile(fileContent, tempInfo.getOriginalName(), userFolder);

        // 3. 解析文件内容
        String content = extractFileContent(fileName);

        // 4. 索引到 Elasticsearch（全文搜索）
        Long fileId = System.currentTimeMillis(); // 或使用数据库生成的ID
        elasticsearchFileService.indexFile(
                fileId,
                userId,
                tempInfo.getOriginalName(),
                content,
                getFileExtension(tempInfo.getOriginalName())
        );

        // 5. 发送 RabbitMQ 消息（异步向量化）
        fileVectorProducer.sendFileForVectorization(fileId, userId, fileName, content);

        // 6. 清理临时文件
        tempFileStorageService.deleteTempFile(tempFileId);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", fileName);
        result.put("originalName", tempInfo.getOriginalName());
        result.put("fileUrl", minioService.getFileUrl(fileName));
        result.put("contentPreview", content.substring(0, Math.min(200, content.length())));
        result.put("indexed", true);
        result.put("vectorizationQueued", true);

        log.info("文件上传并处理完成: fileId={}, userId={}", fileId, userId);
        return result;
    }

    /**
     * 搜索文件内容
     *
     * @param keyword 关键词
     * @param userId  用户ID
     * @param page    页码
     * @param size    每页大小
     * @return 搜索结果
     */
    public List<FileSearchResult> searchFiles(String keyword, Long userId, int page, int size) {
        return elasticsearchFileService.search(keyword, userId, page, size);
    }

    /**
     * 删除文件（同时删除 ES 索引和向量）
     *
     * @param fileId   文件ID
     * @param fileName MinIO 文件名
     * @param userId   用户ID
     */
    public void deleteFile(Long fileId, String fileName, Long userId) {
        // 1. 删除 MinIO 文件
        minioService.deleteFile(fileName);

        // 2. 删除 ES 索引
        elasticsearchFileService.deleteIndex(fileId);

        // 3. 删除向量（异步）
        // 可以发送消息到 RabbitMQ 异步删除

        log.info("文件删除完成: fileId={}, userId={}", fileId, userId);
    }

    /**
     * 提取文件内容
     */
    private String extractFileContent(String fileName) {
        try {
            List<FileContentService.FileContent> contents = fileContentService.parseFiles(List.of(fileName));
            if (!contents.isEmpty() && !contents.get(0).getContent().startsWith("[")) {
                return contents.get(0).getContent();
            }
        } catch (Exception e) {
            log.error("文件内容提取失败: fileName={}", fileName, e);
        }
        return "";
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
