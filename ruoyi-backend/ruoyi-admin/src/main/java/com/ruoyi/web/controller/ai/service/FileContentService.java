package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件内容解析服务
 * 从 MinIO 下载文件，解析为纯文本
 */
@Service
public class FileContentService {

    private static final Logger log = LoggerFactory.getLogger(FileContentService.class);
    private static final int MAX_CHARS_PER_FILE = 3000; // 减小到3000字符，避免超出token限制
    private static final int MAX_FILE_SIZE_MB = 5; // 最大解析5MB文件

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    /**
     * 批量解析文件
     *
     * @param fileNames 文件名列表（MinIO 路径）
     * @return 每个文件的文本内容
     */
    public List<FileContent> parseFiles(List<String> fileNames) {
        List<FileContent> results = new ArrayList<>();
        if (fileNames == null || fileNames.isEmpty()) {
            return results;
        }
        for (String fileName : fileNames) {
            try {
                String text = parseSingleFile(fileName);
                results.add(new FileContent(fileName, text));
            } catch (Exception e) {
                log.warn("文件解析失败: {} - {}", fileName, e.getMessage());
                results.add(new FileContent(fileName, "[解析失败: " + e.getMessage() + "]"));
            }
        }
        return results;
    }

    private String parseSingleFile(String fileName) throws Exception {
        String ext = getExtension(fileName).toLowerCase();
        
        log.info("开始解析文件: {}, 类型: {}", fileName, ext);
        long startTime = System.currentTimeMillis();

        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(fileName)
                        .build())) {

            // 检查文件大小
            int available = in.available();
            if (available > MAX_FILE_SIZE_MB * 1024 * 1024) {
                return "[文件过大，仅支持解析 " + MAX_FILE_SIZE_MB + "MB 以内的文件]";
            }

            String text;
            switch (ext) {
                case "pdf":
                    text = parsePdf(in);
                    break;
                case "docx":
                    text = parseDocx(in);
                    break;
                case "txt":
                case "md":
                    text = parseText(in);
                    break;
                default:
                    text = "[不支持解析的文件类型: " + ext + "]";
            }

            long endTime = System.currentTimeMillis();
            log.info("文件解析完成: {}, 耗时: {}ms, 原始长度: {}", fileName, (endTime - startTime), text.length());

            if (text.length() > MAX_CHARS_PER_FILE) {
                text = text.substring(0, MAX_CHARS_PER_FILE) + "\n\n... [内容已截断，仅展示前" + MAX_CHARS_PER_FILE + "字符]";
            }
            return text;
        }
    }

    private String parsePdf(InputStream in) throws Exception {
        try (PDDocument doc = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDocx(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String parseText(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf(".");
        return idx == -1 ? "" : fileName.substring(idx + 1);
    }

    /**
     * 解析结果
     */
    public static class FileContent {
        private final String fileName;
        private final String content;

        public FileContent(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() { return fileName; }
        public String getContent() { return content; }
    }
}
