package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
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
 */
@RestController
@RequestMapping("/ai/chat")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SseChatController {

    private static final Logger log = LoggerFactory.getLogger(SseChatController.class);

    @Resource
    private EnhancedChatServiceImpl enhancedChatService;

    /**
     * SSE 流式对话接口
     *
     * 前端使用示例：
     * const eventSource = new EventSource('/ai/chat/stream?userId=1&message=你好');
     * eventSource.onmessage = (event) => {
     *     const data = JSON.parse(event.data);
     *     if (data.token) {
     *         // 逐字显示
     *         appendToken(data.token);
     *     }
     * };
     *
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param message 用户消息
     * @param fileNames 关联文件名列表（可选）
     * @param scene 场景（可选，默认default）
     * @param resumeContext 简历上下文JSON（仅interview场景使用）
     * @return SseEmitter
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "fileNames", required = false) List<String> fileNames,
            @RequestParam(value = "scene", defaultValue = "default") String scene,
            @RequestParam(value = "resumeContext", required = false) String resumeContext) {

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
     * POST 方式 SSE 流式对话（支持更长消息）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "fileNames", required = false) List<String> fileNames,
            @RequestParam(value = "scene", defaultValue = "default") String scene,
            @RequestParam(value = "resumeContext", required = false) String resumeContext) {

        return streamChat(userId, sessionId, message, fileNames, scene, resumeContext);
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
