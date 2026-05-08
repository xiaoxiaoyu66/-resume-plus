package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.service.MinioService;
import com.ruoyi.web.controller.ai.service.TempFileStorageService;
import com.ruoyi.web.controller.ai.service.TempFileStorageService.TempFileInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * AI 文件上传控制器
 * 支持上传文件到临时存储（Redis），仅在真正使用时才保存到MinIO
 */
@RestController
@RequestMapping("/ai/file")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Resource
    private MinioService minioService;

    @Resource
    private TempFileStorageService tempFileStorageService;

    // 支持的文件类型
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "md"
    ));

    // 最大文件大小 10MB（临时存储限制5MB，这里保持兼容）
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 模拟会话文件存储（存储临时文件ID）
    private static final Map<String, List<String>> sessionTempFilesMap = new HashMap<>();

    /**
     * 上传单个文件到临时存储
     *
     * @param file   文件
     * @param sessionId 会话ID（可选）
     * @return 临时文件信息
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) String sessionId) {

        Long userId = SecurityUtils.getUserId();

        // 校验文件
        AjaxResult validateResult = validateFile(file);
        if (validateResult != null) {
            return validateResult;
        }

        try {
            // 保存到临时存储（Redis）
            String tempFileId = tempFileStorageService.saveTempFile(file, userId);

            // 获取临时文件信息
            TempFileInfo fileInfo = tempFileStorageService.getTempFileInfo(tempFileId);

            Map<String, Object> result = new HashMap<>();
            result.put("tempFileId", tempFileId);
            result.put("originalName", fileInfo.getOriginalName());
            result.put("fileSize", fileInfo.getFileSize());
            result.put("contentType", fileInfo.getContentType());
            result.put("uploadTime", LocalDateTime.now().toString());
            result.put("isTemp", true); // 标记为临时文件

            // 如果有会话ID，将临时文件ID添加到会话
            if (sessionId != null && !sessionId.isEmpty()) {
                addTempFileToSession(sessionId, tempFileId);
            }

            log.info("用户 {} 上传文件到临时存储: tempFileId={}, name={}", userId, tempFileId, fileInfo.getOriginalName());
            return AjaxResult.success("上传成功", result);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件到临时存储
     *
     * @param files  文件列表
     * @param sessionId 会话ID（可选）
     * @return 上传结果列表
     */
    @PostMapping("/upload/batch")
    public AjaxResult uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "sessionId", required = false) String sessionId) {

        Long userId = SecurityUtils.getUserId();
        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            // 校验文件
            AjaxResult validateResult = validateFile(file);
            if (validateResult != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("originalName", file.getOriginalFilename());
                error.put("success", false);
                error.put("message", validateResult.get("msg"));
                results.add(error);
                continue;
            }

            try {
                // 保存到临时存储
                String tempFileId = tempFileStorageService.saveTempFile(file, userId);
                TempFileInfo fileInfo = tempFileStorageService.getTempFileInfo(tempFileId);

                Map<String, Object> success = new HashMap<>();
                success.put("tempFileId", tempFileId);
                success.put("originalName", fileInfo.getOriginalName());
                success.put("fileSize", fileInfo.getFileSize());
                success.put("success", true);
                success.put("uploadTime", LocalDateTime.now().toString());
                success.put("isTemp", true);
                results.add(success);

                // 如果有会话ID，将临时文件ID添加到会话
                if (sessionId != null && !sessionId.isEmpty()) {
                    addTempFileToSession(sessionId, tempFileId);
                }

            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("originalName", file.getOriginalFilename());
                error.put("success", false);
                error.put("message", e.getMessage());
                results.add(error);
            }
        }

        return AjaxResult.success("批量上传完成", results);
    }

    /**
     * 将临时文件保存到MinIO（真正保存）
     * 在调用AI API时调用此方法
     *
     * @param tempFileId 临时文件ID
     * @param folder 目标文件夹
     * @return 保存后的文件信息
     */
    @PostMapping("/persist")
    public AjaxResult persistFile(
            @RequestParam("tempFileId") String tempFileId,
            @RequestParam(value = "folder", required = false, defaultValue = "ai-uploads/") String folder) {

        Long userId = SecurityUtils.getUserId();

        // 验证文件归属
        if (!tempFileStorageService.validateFileOwnership(tempFileId, userId)) {
            return AjaxResult.error("无权访问该文件或文件已过期");
        }

        try {
            // 获取临时文件信息
            TempFileInfo tempInfo = tempFileStorageService.getTempFileInfo(tempFileId);
            if (tempInfo == null) {
                return AjaxResult.error("临时文件不存在或已过期");
            }

            // 获取文件内容
            byte[] fileContent = tempFileStorageService.getTempFileBytes(tempFileId);

            // 上传到MinIO
            String userFolder = folder + userId + "/";
            String fileName = minioService.uploadFile(fileContent, tempInfo.getOriginalName(), userFolder);
            String fileUrl = minioService.getFileUrl(fileName);

            // 删除临时文件
            tempFileStorageService.deleteTempFile(tempFileId);

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("originalName", tempInfo.getOriginalName());
            result.put("fileUrl", fileUrl);
            result.put("fileSize", tempInfo.getFileSize());
            result.put("isTemp", false);

            log.info("用户 {} 将临时文件保存到MinIO: fileName={}", userId, fileName);
            return AjaxResult.success("保存成功", result);

        } catch (Exception e) {
            log.error("保存文件失败: {}", e.getMessage(), e);
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 批量将临时文件保存到MinIO
     *
     * @param tempFileIds 临时文件ID列表
     * @param folder 目标文件夹
     * @return 保存后的文件信息列表
     */
    @PostMapping("/persist/batch")
    public AjaxResult persistFiles(
            @RequestParam("tempFileIds") List<String> tempFileIds,
            @RequestParam(value = "folder", required = false, defaultValue = "ai-uploads/") String folder) {

        Long userId = SecurityUtils.getUserId();
        List<Map<String, Object>> results = new ArrayList<>();

        for (String tempFileId : tempFileIds) {
            // 验证文件归属
            if (!tempFileStorageService.validateFileOwnership(tempFileId, userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("tempFileId", tempFileId);
                error.put("success", false);
                error.put("message", "无权访问或文件已过期");
                results.add(error);
                continue;
            }

            try {
                TempFileInfo tempInfo = tempFileStorageService.getTempFileInfo(tempFileId);
                if (tempInfo == null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("tempFileId", tempFileId);
                    error.put("success", false);
                    error.put("message", "临时文件不存在或已过期");
                    results.add(error);
                    continue;
                }

                byte[] fileContent = tempFileStorageService.getTempFileBytes(tempFileId);
                String userFolder = folder + userId + "/";
                String fileName = minioService.uploadFile(fileContent, tempInfo.getOriginalName(), userFolder);
                String fileUrl = minioService.getFileUrl(fileName);

                tempFileStorageService.deleteTempFile(tempFileId);

                Map<String, Object> success = new HashMap<>();
                success.put("fileName", fileName);
                success.put("originalName", tempInfo.getOriginalName());
                success.put("fileUrl", fileUrl);
                success.put("fileSize", tempInfo.getFileSize());
                success.put("success", true);
                success.put("isTemp", false);
                results.add(success);

            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("tempFileId", tempFileId);
                error.put("success", false);
                error.put("message", e.getMessage());
                results.add(error);
            }
        }

        return AjaxResult.success("批量保存完成", results);
    }

    /**
     * 获取文件预签名URL（临时访问）
     * 优先从MinIO获取，如果是临时文件则不支持
     *
     * @param fileName 文件名（MinIO路径）
     * @return 预签名URL
     */
    @GetMapping("/url")
    public AjaxResult getPresignedUrl(@RequestParam("fileName") String fileName) {
        try {
            // 校验文件归属（简单校验：路径包含用户ID）
            Long userId = SecurityUtils.getUserId();
            if (!fileName.contains("/" + userId + "/")) {
                return AjaxResult.error("无权访问该文件");
            }

            String url = minioService.getPresignedUrl(fileName);
            return AjaxResult.success("获取成功", url);
        } catch (Exception e) {
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名（如果是临时文件传tempFileId）
     * @param isTemp 是否为临时文件
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public AjaxResult deleteFile(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "isTemp", defaultValue = "false") boolean isTemp) {

        Long userId = SecurityUtils.getUserId();

        try {
            if (isTemp) {
                // 删除临时文件
                if (!tempFileStorageService.validateFileOwnership(fileName, userId)) {
                    return AjaxResult.error("无权删除该文件");
                }
                tempFileStorageService.deleteTempFile(fileName);
            } else {
                // 删除MinIO文件
                if (!fileName.contains("/" + userId + "/")) {
                    return AjaxResult.error("无权删除该文件");
                }
                minioService.deleteFile(fileName);
            }
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件列表（MinIO中的文件）
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param timeRange 时间范围：1hour, today, week, month, custom
     * @param startTime 自定义开始时间（ISO格式：2024-01-01T00:00:00）
     * @param endTime 自定义结束时间（ISO格式：2024-01-01T23:59:59）
     * @param keyword 搜索关键词
     * @return 文件列表
     */
    @GetMapping("/list")
    public AjaxResult listUserFiles(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "keyword", required = false) String keyword) {

        Long userId = SecurityUtils.getUserId();
        String prefix = "ai-uploads/" + userId + "/";

        try {
            // 获取所有文件
            List<Map<String, Object>> allFiles = minioService.listFilesWithMetadata(prefix);

            // 根据时间范围筛选
            if (timeRange != null && !timeRange.isEmpty()) {
                if ("custom".equals(timeRange) && startTime != null && endTime != null) {
                    // 自定义时间范围
                    allFiles = filterByCustomTimeRange(allFiles, startTime, endTime);
                } else {
                    // 预设时间范围
                    allFiles = filterByTimeRange(allFiles, timeRange);
                }
            }

            // 根据关键词筛选
            if (keyword != null && !keyword.isEmpty()) {
                allFiles = filterByKeyword(allFiles, keyword);
            }

            // 按上传时间倒序排序
            allFiles.sort((a, b) -> {
                String timeA = (String) a.getOrDefault("uploadTime", "");
                String timeB = (String) b.getOrDefault("uploadTime", "");
                return timeB.compareTo(timeA);
            });

            // 分页
            int total = allFiles.size();
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            List<Map<String, Object>> pageList = new ArrayList<>();
            if (start < total) {
                pageList = allFiles.subList(start, end);
            }

            // 为每个文件生成URL
            for (Map<String, Object> file : pageList) {
                String fName = (String) file.get("fileName");
                file.put("fileUrl", minioService.getFileUrl(fName));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("list", pageList);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("pages", (int) Math.ceil((double) total / pageSize));

            return AjaxResult.success("获取成功", result);

        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的临时文件列表
     *
     * @return 临时文件列表
     */
    @GetMapping("/temp/list")
    public AjaxResult listTempFiles() {
        Long userId = SecurityUtils.getUserId();

        try {
            List<TempFileInfo> tempFiles = tempFileStorageService.getUserTempFiles(userId);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (TempFileInfo info : tempFiles) {
                Map<String, Object> item = new HashMap<>();
                item.put("tempFileId", info.getFileId());
                item.put("originalName", info.getOriginalName());
                item.put("fileSize", info.getFileSize());
                item.put("contentType", info.getContentType());
                item.put("uploadTime", LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(info.getUploadTime()),
                        java.time.ZoneId.systemDefault()).toString());
                item.put("isTemp", true);
                resultList.add(item);
            }

            return AjaxResult.success("获取成功", resultList);
        } catch (Exception e) {
            log.error("获取临时文件列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前会话的临时文件列表
     *
     * @param sessionId 会话ID
     * @return 临时文件ID列表
     */
    @GetMapping("/session")
    public AjaxResult getSessionFiles(@RequestParam("sessionId") String sessionId) {
        List<String> tempFileIds = sessionTempFilesMap.getOrDefault(sessionId, new ArrayList<>());

        // 获取详细信息
        List<Map<String, Object>> files = new ArrayList<>();
        for (String tempFileId : tempFileIds) {
            TempFileInfo info = tempFileStorageService.getTempFileInfo(tempFileId);
            if (info != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("tempFileId", tempFileId);
                item.put("originalName", info.getOriginalName());
                item.put("fileSize", info.getFileSize());
                item.put("isTemp", true);
                files.add(item);
            }
        }

        return AjaxResult.success("获取成功", files);
    }

    /**
     * 根据时间范围筛选文件
     * 只显示选中时间段内的文件（精确匹配）
     */
    private List<Map<String, Object>> filterByTimeRange(List<Map<String, Object>> files, String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime;

        switch (timeRange) {
            case "1hour":
                // 最近1小时：从当前时间前1小时到当前时间
                startTime = now.minus(1, ChronoUnit.HOURS);
                endTime = now;
                break;
            case "today":
                // 今天：从00:00到23:59:59
                startTime = now.truncatedTo(ChronoUnit.DAYS);
                endTime = startTime.plus(1, ChronoUnit.DAYS).minusSeconds(1);
                break;
            case "week":
                // 本周：从本周一到本周日
                startTime = now.with(java.time.DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
                endTime = startTime.plus(6, ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS).plus(59, ChronoUnit.MINUTES).plus(59, ChronoUnit.SECONDS);
                break;
            case "month":
                // 本月：从本月1号到本月最后一天
                startTime = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                endTime = startTime.plus(1, ChronoUnit.MONTHS).minusSeconds(1);
                break;
            default:
                return files;
        }

        return files.stream()
                .filter(file -> {
                    String uploadTime = (String) file.get("uploadTime");
                    if (uploadTime == null) return false;
                    try {
                        LocalDateTime fileTime = LocalDateTime.parse(uploadTime);
                        // 只显示在选中时间段内的文件（包含边界）
                        return (fileTime.isAfter(startTime) || fileTime.isEqual(startTime)) 
                                && (fileTime.isBefore(endTime) || fileTime.isEqual(endTime));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据自定义时间范围筛选文件
     * @param startTime ISO格式时间字符串（如：2024-01-01T00:00:00）
     * @param endTime ISO格式时间字符串（如：2024-01-01T23:59:59）
     */
    private List<Map<String, Object>> filterByCustomTimeRange(List<Map<String, Object>> files, String startTime, String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            
            log.info("自定义时间范围筛选: {} 至 {}", startTime, endTime);
            
            return files.stream()
                    .filter(file -> {
                        String uploadTime = (String) file.get("uploadTime");
                        if (uploadTime == null) return false;
                        try {
                            LocalDateTime fileTime = LocalDateTime.parse(uploadTime);
                            // 包含边界的时间范围筛选
                            return (fileTime.isAfter(start) || fileTime.isEqual(start)) 
                                    && (fileTime.isBefore(end) || fileTime.isEqual(end));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("自定义时间范围解析失败: startTime={}, endTime={}", startTime, endTime, e);
            return files;
        }
    }

    /**
     * 根据关键词筛选文件
     */
    private List<Map<String, Object>> filterByKeyword(List<Map<String, Object>> files, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return files.stream()
                .filter(file -> {
                    String originalName = (String) file.get("originalName");
                    String fileName = (String) file.get("fileName");
                    return (originalName != null && originalName.toLowerCase().contains(lowerKeyword)) ||
                           (fileName != null && fileName.toLowerCase().contains(lowerKeyword));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 添加临时文件ID到会话
     */
    private void addTempFileToSession(String sessionId, String tempFileId) {
        sessionTempFilesMap.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(tempFileId);
    }

    /**
     * 校验文件
     */
    private AjaxResult validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return AjaxResult.error("文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return AjaxResult.error("文件大小不能超过5MB");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return AjaxResult.error("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_TYPES.contains(extension.toLowerCase())) {
            return AjaxResult.error("不支持的文件类型，仅支持: " + String.join(", ", ALLOWED_TYPES));
        }

        return null;
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
