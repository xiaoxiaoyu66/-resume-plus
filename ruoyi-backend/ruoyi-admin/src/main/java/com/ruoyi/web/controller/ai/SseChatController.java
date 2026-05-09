package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.service.ChatCacheService;
import com.ruoyi.web.controller.ai.service.impl.EnhancedChatServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * SSE 流式对话控制器
 * 所有接口从 JWT 获取用户身份，拒绝客户端传入 userId
 */
@RestController
@RequestMapping("/ai/chat")
public class SseChatController {

    private static final Logger log = LoggerFactory.getLogger(SseChatController.class);

    @Resource
    private EnhancedChatServiceImpl enhancedChatService;

    /**
     * SSE 流式对话接口（GET）
     * 用户身份从 JWT 中获取，前端无需传 userId
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "fileNames", required = false) List<String> fileNames,
            @RequestParam(value = "scene", defaultValue = "default") String scene,
            @RequestParam(value = "resumeContext", required = false) String resumeContext) {

        Long userId = SecurityUtils.getUserId();
        log.info("SSE流式对话请求: userId={}, sessionId={}, scene={}", userId, sessionId, scene);

        try {
            return enhancedChatService.streamChat(userId, sessionId, message, fileNames, scene, resumeContext);
        } catch (Exception e) {
            log.error("SSE流式对话异常", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * SSE 流式对话接口（POST，支持更长消息）
     * 用户身份从 JWT 中获取
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "fileNames", required = false) List<String> fileNames,
            @RequestParam(value = "scene", defaultValue = "default") String scene,
            @RequestParam(value = "resumeContext", required = false) String resumeContext) {

        return streamChat(sessionId, message, fileNames, scene, resumeContext);
    }

    /**
     * 获取缓存统计
     */
    @GetMapping("/cache/stats")
    public AjaxResult getCacheStats() {
        ChatCacheService.CacheStats stats = enhancedChatService.getCacheStats();
        return AjaxResult.success(stats);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public AjaxResult health() {
        return AjaxResult.success("SSE服务正常运行");
    }
}
