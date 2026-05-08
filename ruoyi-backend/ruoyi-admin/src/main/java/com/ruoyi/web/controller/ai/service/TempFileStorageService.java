package com.ruoyi.web.controller.ai.service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.web.controller.ai.config.AiConstants;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 临时文件存储服务
 * 使用Redis存储文件内容，延迟到真正需要时才保存到MinIO
 */
@Service
public class TempFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(TempFileStorageService.class);

    @Resource
    private RedisCache redisCache;

    // Redis key前缀
    private static final String TEMP_FILE_PREFIX = AiConstants.TEMP_FILE_PREFIX;
    private static final String TEMP_FILE_INDEX_PREFIX = AiConstants.TEMP_FILE_INDEX_PREFIX;

    // 临时文件过期时间（24小时）
    private static final long TEMP_FILE_EXPIRE_HOURS = AiConstants.TEMP_FILE_EXPIRE_HOURS;

    // 单文件最大大小（5MB）
    private static final long MAX_FILE_SIZE = AiConstants.MAX_FILE_SIZE;

    /**
     * 临时文件信息
     */
    public static class TempFileInfo {
        private String fileId;
        private String originalName;
        private String contentType;
        private long fileSize;
        private long uploadTime;
        private Long userId;

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }

        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public long getUploadTime() { return uploadTime; }
        public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    /**
     * 保存临时文件到Redis
     *
     * @param file   文件
     * @param userId 用户ID
     * @return 临时文件ID
     */
    public String saveTempFile(MultipartFile file, Long userId) throws IOException {
        // 生成临时文件ID
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = TEMP_FILE_PREFIX + fileId;

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("文件大小超过限制（最大5MB）");
        }

        // 读取文件内容
        byte[] fileContent = file.getBytes();

        // 构建文件信息
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("originalName", file.getOriginalFilename());
        fileInfo.put("contentType", file.getContentType());
        fileInfo.put("fileSize", file.getSize());
        fileInfo.put("uploadTime", System.currentTimeMillis());
        fileInfo.put("userId", userId);
        fileInfo.put("content", Base64.getEncoder().encodeToString(fileContent));

        // 保存到Redis，设置24小时过期
        redisCache.redisTemplate.opsForHash().putAll(redisKey, fileInfo);
        redisCache.expire(redisKey, TEMP_FILE_EXPIRE_HOURS, TimeUnit.HOURS);

        // 添加到用户临时文件索引
        addToUserIndex(userId, fileId);

        log.info("临时文件已保存到Redis: fileId={}, userId={}, size={}", fileId, userId, file.getSize());
        return fileId;
    }

    /**
     * 批量保存临时文件
     *
     * @param files  文件列表
     * @param userId 用户ID
     * @return 临时文件ID列表
     */
    public List<String> saveTempFiles(MultipartFile[] files, Long userId) throws IOException {
        List<String> fileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            fileIds.add(saveTempFile(file, userId));
        }
        return fileIds;
    }

    /**
     * 获取临时文件信息
     *
     * @param fileId 临时文件ID
     * @return 文件信息
     */
    public TempFileInfo getTempFileInfo(String fileId) {
        String redisKey = TEMP_FILE_PREFIX + fileId;
        Map<Object, Object> fileData = redisCache.redisTemplate.opsForHash().entries(redisKey);

        if (fileData == null || fileData.isEmpty()) {
            return null;
        }

        TempFileInfo info = new TempFileInfo();
        info.setFileId(fileId);
        info.setOriginalName((String) fileData.get("originalName"));
        info.setContentType((String) fileData.get("contentType"));
        info.setFileSize(Long.parseLong(fileData.get("fileSize").toString()));
        info.setUploadTime(Long.parseLong(fileData.get("uploadTime").toString()));
        info.setUserId(Long.parseLong(fileData.get("userId").toString()));

        return info;
    }

    /**
     * 获取临时文件内容
     *
     * @param fileId 临时文件ID
     * @return 文件内容输入流
     */
    public InputStream getTempFileContent(String fileId) {
        String redisKey = TEMP_FILE_PREFIX + fileId;
        Map<Object, Object> fileData = redisCache.redisTemplate.opsForHash().entries(redisKey);

        if (fileData == null || fileData.isEmpty()) {
            return null;
        }

        String base64Content = (String) fileData.get("content");
        byte[] content = Base64.getDecoder().decode(base64Content);
        return new ByteArrayInputStream(content);
    }

    /**
     * 获取临时文件内容（字节数组）
     *
     * @param fileId 临时文件ID
     * @return 文件内容
     */
    public byte[] getTempFileBytes(String fileId) {
        String redisKey = TEMP_FILE_PREFIX + fileId;
        Map<Object, Object> fileData = redisCache.redisTemplate.opsForHash().entries(redisKey);

        if (fileData == null || fileData.isEmpty()) {
            return null;
        }

        String base64Content = (String) fileData.get("content");
        return Base64.getDecoder().decode(base64Content);
    }

    /**
     * 删除临时文件
     *
     * @param fileId 临时文件ID
     */
    public void deleteTempFile(String fileId) {
        String redisKey = TEMP_FILE_PREFIX + fileId;

        // 获取文件信息
        TempFileInfo info = getTempFileInfo(fileId);
        if (info != null) {
            // 从用户索引中移除
            removeFromUserIndex(info.getUserId(), fileId);
        }

        // 删除Redis数据
        redisCache.redisTemplate.delete(redisKey);
        log.info("临时文件已删除: fileId={}", fileId);
    }

    /**
     * 批量删除临时文件
     *
     * @param fileIds 临时文件ID列表
     */
    public void deleteTempFiles(List<String> fileIds) {
        for (String fileId : fileIds) {
            deleteTempFile(fileId);
        }
    }

    /**
     * 获取用户的所有临时文件ID
     *
     * @param userId 用户ID
     * @return 临时文件ID列表
     */
    public List<String> getUserTempFileIds(Long userId) {
        String indexKey = TEMP_FILE_INDEX_PREFIX + userId;
        Set<Object> fileIds = redisCache.redisTemplate.opsForSet().members(indexKey);

        if (fileIds == null || fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Object id : fileIds) {
            result.add((String) id);
        }
        return result;
    }

    /**
     * 获取用户的所有临时文件信息
     *
     * @param userId 用户ID
     * @return 临时文件信息列表
     */
    public List<TempFileInfo> getUserTempFiles(Long userId) {
        List<String> fileIds = getUserTempFileIds(userId);
        List<TempFileInfo> files = new ArrayList<>();

        for (String fileId : fileIds) {
            TempFileInfo info = getTempFileInfo(fileId);
            if (info != null) {
                files.add(info);
            }
        }

        // 按上传时间倒序排序
        files.sort((a, b) -> Long.compare(b.getUploadTime(), a.getUploadTime()));
        return files;
    }

    /**
     * 清理用户所有临时文件
     *
     * @param userId 用户ID
     */
    public void clearUserTempFiles(Long userId) {
        List<String> fileIds = getUserTempFileIds(userId);
        for (String fileId : fileIds) {
            String redisKey = TEMP_FILE_PREFIX + fileId;
            redisCache.redisTemplate.delete(redisKey);
        }

        String indexKey = TEMP_FILE_INDEX_PREFIX + userId;
        redisCache.redisTemplate.delete(indexKey);

        log.info("用户临时文件已清空: userId={}, count={}", userId, fileIds.size());
    }

    /**
     * 将文件添加到用户索引
     */
    private void addToUserIndex(Long userId, String fileId) {
        String indexKey = TEMP_FILE_INDEX_PREFIX + userId;
        redisCache.redisTemplate.opsForSet().add(indexKey, fileId);
        redisCache.expire(indexKey, TEMP_FILE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 从用户索引中移除文件
     */
    private void removeFromUserIndex(Long userId, String fileId) {
        String indexKey = TEMP_FILE_INDEX_PREFIX + userId;
        redisCache.redisTemplate.opsForSet().remove(indexKey, fileId);
    }

    /**
     * 验证临时文件归属
     *
     * @param fileId 临时文件ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    public boolean validateFileOwnership(String fileId, Long userId) {
        TempFileInfo info = getTempFileInfo(fileId);
        return info != null && info.getUserId().equals(userId);
    }
}
