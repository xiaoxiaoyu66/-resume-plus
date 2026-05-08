package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.service.ElasticsearchFileService.FileSearchResult;
import com.ruoyi.web.controller.ai.service.EnhancedFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 增强版文件控制器
 * 集成 Elasticsearch 全文搜索
 */
@RestController
@RequestMapping("/ai/file/enhanced")
public class EnhancedFileController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(EnhancedFileController.class);

    @Autowired
    private EnhancedFileUploadService enhancedFileUploadService;

    /**
     * 上传文件（集成 ES 索引和 RabbitMQ 向量化）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getUserId();

        try {
            Map<String, Object> result = enhancedFileUploadService.uploadAndProcessFile(file, userId);
            return AjaxResult.success("上传成功", result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件内容（全文搜索）
     */
    @GetMapping("/search")
    public AjaxResult searchFiles(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();

        try {
            List<FileSearchResult> results = enhancedFileUploadService.searchFiles(keyword, userId, page, size);
            return AjaxResult.success("搜索成功", results);
        } catch (Exception e) {
            log.error("文件搜索失败", e);
            return AjaxResult.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（同时删除 ES 索引）
     */
    @DeleteMapping("/delete")
    public AjaxResult deleteFile(
            @RequestParam("fileId") Long fileId,
            @RequestParam("fileName") String fileName) {
        Long userId = SecurityUtils.getUserId();

        try {
            enhancedFileUploadService.deleteFile(fileId, fileName, userId);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }
}
