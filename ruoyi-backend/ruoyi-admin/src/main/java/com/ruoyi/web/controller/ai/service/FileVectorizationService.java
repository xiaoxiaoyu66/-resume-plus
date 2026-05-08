package com.ruoyi.web.controller.ai.service;

import com.ruoyi.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件向量化服务
 * 将上传的文件解析、分块、向量化并存储到 PGVector
 */
@Service
public class FileVectorizationService {

    private static final Logger log = LoggerFactory.getLogger(FileVectorizationService.class);

    @Resource
    private FileContentService fileContentService;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private VectorService vectorService;

    // 分块大小（字符数）
    private static final int CHUNK_SIZE = 800;
    // 重叠大小
    private static final int CHUNK_OVERLAP = 100;

    /**
     * 异步向量化文件
     *
     * @param fileId   文件ID（业务系统生成的ID）
     * @param fileName MinIO中的文件名
     * @param userId   用户ID
     */
    @Async
    public void vectorizeFileAsync(Long fileId, String fileName, Long userId) {
        try {
            log.info("开始异步向量化文件: fileId={}, fileName={}", fileId, fileName);

            // 1. 解析文件内容
            List<FileContentService.FileContent> contents = fileContentService.parseFiles(List.of(fileName));
            if (contents.isEmpty() || contents.get(0).getContent().startsWith("[")) {
                log.warn("文件解析失败或为空: fileId={}", fileId);
                return;
            }

            String fullText = contents.get(0).getContent();

            // 2. 文本分块
            List<String> chunks = embeddingService.chunkTextSmart(fullText, CHUNK_SIZE);
            log.info("文件分块完成: fileId={}, 共{}块", fileId, chunks.size());

            // 3. 保存向量
            int savedCount = vectorService.saveDocumentVectors(fileId, userId, chunks);
            log.info("文件向量化完成: fileId={}, 保存{}块向量", fileId, savedCount);

        } catch (Exception e) {
            log.error("文件向量化失败: fileId={}", fileId, e);
        }
    }

    /**
     * 同步向量化文件（小文件使用）
     *
     * @param fileId   文件ID
     * @param fileName MinIO中的文件名
     * @param userId   用户ID
     * @return 保存的向量数量
     */
    public int vectorizeFile(Long fileId, String fileName, Long userId) {
        try {
            log.info("开始同步向量化文件: fileId={}, fileName={}", fileId, fileName);

            // 1. 解析文件内容
            List<FileContentService.FileContent> contents = fileContentService.parseFiles(List.of(fileName));
            if (contents.isEmpty() || contents.get(0).getContent().startsWith("[")) {
                log.warn("文件解析失败或为空: fileId={}", fileId);
                return 0;
            }

            String fullText = contents.get(0).getContent();

            // 2. 文本分块
            List<String> chunks = embeddingService.chunkTextSmart(fullText, CHUNK_SIZE);
            log.info("文件分块完成: fileId={}, 共{}块", fileId, chunks.size());

            // 3. 保存向量
            int savedCount = vectorService.saveDocumentVectors(fileId, userId, chunks);
            log.info("文件向量化完成: fileId={}, 保存{}块向量", fileId, savedCount);

            return savedCount;

        } catch (Exception e) {
            log.error("文件向量化失败: fileId={}", fileId, e);
            return 0;
        }
    }

    /**
     * 删除文件的向量
     *
     * @param fileId 文件ID
     */
    public void deleteFileVectors(Long fileId) {
        vectorService.deleteByFileId(fileId);
        log.info("删除文件向量: fileId={}", fileId);
    }

    /**
     * 获取文件的向量数量
     *
     * @param fileId 文件ID
     * @return 向量数量
     */
    public int getVectorCount(Long fileId) {
        return vectorService.getVectorCount(fileId);
    }
}
