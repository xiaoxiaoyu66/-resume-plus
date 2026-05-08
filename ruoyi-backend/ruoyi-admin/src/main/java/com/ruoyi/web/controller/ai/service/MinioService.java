package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    /**
     * 上传文件
     *
     * @param file     文件
     * @param folder   文件夹路径（如：documents/）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String fileName = generateFileName(folder, extension);

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("文件上传成功: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（带自定义文件名）
     *
     * @param file     文件
     * @param fileName 自定义文件名
     * @return 文件路径
     */
    public String uploadFile(MultipartFile file, String folder, String fileName) {
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String fullPath = folder + fileName + "." + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("文件上传成功: {}", fullPath);
            return fullPath;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（字节数组方式）
     *
     * @param fileContent 文件内容字节数组
     * @param originalName 原始文件名
     * @param folder 文件夹路径
     * @return 文件路径
     */
    public String uploadFile(byte[] fileContent, String originalName, String folder) {
        try {
            String extension = getFileExtension(originalName);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            // 保留原始文件名：uuid_原始名.扩展名
            String baseName = originalName.contains(".") ?
                    originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
            // 移除路径中不允许的字符
            baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = folder + uuid + "_" + baseName + "." + extension;

            // 推断contentType
            String contentType = inferContentType(extension);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(new java.io.ByteArrayInputStream(fileContent), fileContent.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("文件上传成功: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 推断文件ContentType
     */
    private String inferContentType(String extension) {
        switch (extension.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt": return "text/plain";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "md": return "text/markdown";
            default: return "application/octet-stream";
        }
    }

    /**
     * 获取文件预签名URL（临时访问链接）
     *
     * @param fileName 文件名
     * @return 预签名URL
     */
    public String getPresignedUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .expiry(minioProperties.getExpiry(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件链接失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件直接访问URL（需要存储桶设置为公共读）
     *
     * @param fileName 文件名
     * @return 直接访问URL
     */
    public String getFileUrl(String fileName) {
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + fileName;
    }

    /**
     * 下载文件
     *
     * @param fileName 文件名
     * @return 文件输入流
     */
    public InputStream downloadFile(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
            log.info("文件删除成功: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除文件
     *
     * @param fileNames 文件名列表
     */
    public void deleteFiles(List<String> fileNames) {
        try {
            List<DeleteObject> objects = new ArrayList<>();
            for (String fileName : fileNames) {
                objects.add(new DeleteObject(fileName));
            }

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .objects(objects)
                            .build()
            );
            log.info("批量文件删除成功，数量: {}", fileNames.size());
        } catch (Exception e) {
            log.error("批量文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 列出文件夹下的文件
     *
     * @param prefix 前缀（文件夹路径）
     * @return 文件列表
     */
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                files.add(result.get().objectName());
            }
        } catch (Exception e) {
            log.error("列出文件失败: {}", e.getMessage(), e);
        }
        return files;
    }

    /**
     * 列出文件夹下的文件（带元数据）
     *
     * @param prefix 前缀（文件夹路径）
     * @return 文件列表（包含元数据）
     */
    public List<java.util.Map<String, Object>> listFilesWithMetadata(String prefix) {
        List<java.util.Map<String, Object>> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                java.util.Map<String, Object> fileInfo = new java.util.HashMap<>();
                fileInfo.put("fileName", item.objectName());
                fileInfo.put("fileSize", item.size());
                
                // 从文件名中提取原始文件名（去掉UUID前缀）
                String objectName = item.objectName();
                String originalName = extractOriginalName(objectName);
                fileInfo.put("originalName", originalName);
                
                // 使用最后修改时间作为上传时间
                if (item.lastModified() != null) {
                    fileInfo.put("uploadTime", item.lastModified().toString());
                } else {
                    fileInfo.put("uploadTime", java.time.LocalDateTime.now().toString());
                }
                
                files.add(fileInfo);
            }
        } catch (Exception e) {
            log.error("列出文件失败: {}", e.getMessage(), e);
        }
        return files;
    }

    /**
     * 从文件路径中提取原始文件名
     * 文件格式：uuid_原文件名.扩展名
     */
    private String extractOriginalName(String objectName) {
        if (objectName == null) return "";
        int lastSlash = objectName.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;

        // 格式：uuid_原文件名.扩展名
        if (fileName.length() > 33 && fileName.charAt(32) == '_') {
            return fileName.substring(33); // 跳过 "uuid_"
        }
        return fileName;
    }

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String folder, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return folder + uuid + "." + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
