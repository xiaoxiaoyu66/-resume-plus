package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.domain.ChatMessage;
import com.ruoyi.web.controller.ai.domain.ChatSession;
import com.ruoyi.web.controller.ai.service.IChatService;
import com.ruoyi.web.controller.ai.service.TempFileStorageService;
import com.ruoyi.web.controller.ai.service.MinioService;
import com.ruoyi.web.controller.ai.service.impl.EnhancedChatServiceImpl;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * AI 聊天控制器
 */
@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    @Resource
    private IChatService chatService;

    @Resource
    private TempFileStorageService tempFileStorageService;

    @Resource
    private MinioService minioService;

    @Resource
    private EnhancedChatServiceImpl enhancedChatService;

    /**
     * 发送对话消息（非流式）
     *
     * @param body 请求体 { sessionId: "sess_xxx" 或数字, message: "...", tempFileIds: [...], scene: "default/resume/interview/career" }
     */
    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getUserId();
        String message = (String) body.get("message");
        
        // 接收已保存的文件名列表
        @SuppressWarnings("unchecked")
        List<String> fileNames = body.get("fileNames") != null
                ? (List<String>) body.get("fileNames") : new ArrayList<>();
        
        // 允许只发送文件，或只发送文字，或两者都发
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasFiles = !fileNames.isEmpty();
        
        if (!hasMessage && !hasFiles) {
            return AjaxResult.error("消息和文件不能同时为空");
        }

        // 处理 sessionId：前端可能传 "sess_xxx" 字符串或数字
        Long sessionId = null;
        Object rawSessionId = body.get("sessionId");
        if (rawSessionId != null) {
            try {
                String sidStr = rawSessionId.toString();
                if (sidStr.startsWith("sess_")) {
                    sessionId = null; // 前端传 sess_xxx 时创建新会话
                } else {
                    sessionId = Long.valueOf(sidStr);
                }
            } catch (NumberFormatException e) {
                sessionId = null; // 解析失败时创建新会话
            }
        }

        // 获取场景（默认default）
        String scene = (String) body.getOrDefault("scene", "default");

        // 接收临时文件ID列表
        @SuppressWarnings("unchecked")
        List<String> tempFileIds = body.get("tempFileIds") != null
                ? (List<String>) body.get("tempFileIds") : new ArrayList<>();

        System.out.println("[ChatController] 接收到的参数: tempFileIds=" + tempFileIds + ", fileNames=" + fileNames);

        try {
            // 如果有临时文件，先保存到MinIO
            List<String> persistedFileNames = new ArrayList<>();
            // 过滤掉 null 元素，避免 FileContentService 解析时报 NPE
            for (String fn : fileNames) {
                if (fn != null && !fn.isEmpty()) {
                    persistedFileNames.add(fn);
                }
            }
            if (!tempFileIds.isEmpty()) {
                List<String> savedFiles = persistTempFiles(tempFileIds, userId);
                System.out.println("[ChatController] 临时文件已保存到MinIO: " + savedFiles);
                persistedFileNames.addAll(savedFiles);
            }

            System.out.println("[ChatController] 最终传递给chatService的文件列表: " + persistedFileNames);

            Map<String, Object> result = chatService.chatWithScene(userId, sessionId, message, persistedFileNames, scene);
            return AjaxResult.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("AI 回复失败: " + e.getMessage());
        }
    }

    /**
     * 将临时文件保存到MinIO
     *
     * @param tempFileIds 临时文件ID列表
     * @param userId 用户ID
     * @return 保存后的MinIO文件名列表
     */
    private List<String> persistTempFiles(List<String> tempFileIds, Long userId) {
        List<String> savedFileNames = new ArrayList<>();
        String folder = "ai-uploads/";

        System.out.println("[persistTempFiles] 开始处理临时文件: " + tempFileIds);

        for (String tempFileId : tempFileIds) {
            System.out.println("[persistTempFiles] 处理临时文件: " + tempFileId);

            // 验证文件归属
            if (!tempFileStorageService.validateFileOwnership(tempFileId, userId)) {
                System.out.println("[persistTempFiles] 文件归属验证失败: " + tempFileId);
                continue;
            }

            try {
                TempFileStorageService.TempFileInfo tempInfo = tempFileStorageService.getTempFileInfo(tempFileId);
                if (tempInfo == null) {
                    System.out.println("[persistTempFiles] 临时文件信息不存在: " + tempFileId);
                    continue;
                }

                System.out.println("[persistTempFiles] 获取到文件信息: " + tempInfo.getOriginalName());

                // 获取文件内容
                byte[] fileContent = tempFileStorageService.getTempFileBytes(tempFileId);
                System.out.println("[persistTempFiles] 文件内容大小: " + (fileContent != null ? fileContent.length : 0) + " bytes");

                // 上传到MinIO
                String userFolder = folder + userId + "/";
                String fileName = minioService.uploadFile(fileContent, tempInfo.getOriginalName(), userFolder);
                System.out.println("[persistTempFiles] 文件已保存到MinIO: " + fileName);
                savedFileNames.add(fileName);

                // 删除临时文件
                tempFileStorageService.deleteTempFile(tempFileId);

            } catch (Exception e) {
                // 记录错误但继续处理其他文件
                System.err.println("[persistTempFiles] 保存临时文件失败: " + tempFileId + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[persistTempFiles] 处理完成，保存的文件: " + savedFileNames);
        return savedFileNames;
    }

    /**
     * 获取历史会话列表
     */
    @GetMapping("/history")
    public AjaxResult history() {
        Long userId = SecurityUtils.getUserId();
        List<ChatSession> list = chatService.getHistory(userId);
        return AjaxResult.success(list);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/{sessionId}")
    public AjaxResult sessionDetail(@PathVariable Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        try {
            List<ChatMessage> messages = chatService.getSessionDetail(sessionId, userId);
            ChatSession session = enhancedChatService.getSession(sessionId, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("messages", messages);
            result.put("scene", session != null ? session.getScene() : "default");
            result.put("resumeId", session != null ? session.getResumeId() : null);
            return AjaxResult.success(result);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        try {
            chatService.deleteSession(sessionId, userId);
            return AjaxResult.success("删除成功");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 更新会话场景
     */
    @PutMapping("/session/{sessionId}/scene")
    public AjaxResult updateScene(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getUserId();
        String scene = body.get("scene");
        if (scene == null || scene.isEmpty()) {
            return AjaxResult.error("场景不能为空");
        }
        try {
            enhancedChatService.updateSessionScene(sessionId, userId, scene);
            return AjaxResult.success("场景更新成功");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户额度信息
     */
    @GetMapping("/quota")
    public AjaxResult getQuota() {
        Long userId = SecurityUtils.getUserId();
        try {
            Map<String, Object> quota = chatService.getUserQuota(userId);
            return AjaxResult.success(quota);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
