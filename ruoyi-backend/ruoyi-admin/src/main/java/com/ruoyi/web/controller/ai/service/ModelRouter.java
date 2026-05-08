package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.config.DeepSeekConfig;
import com.ruoyi.web.controller.ai.config.SiliconFlowChatConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 模型路由 + 熔断降级
 *
 * 主模型 DeepSeek → 故障时自动切换到 SiliconFlow Qwen 免费模型
 * 连续失败 3 次后熔断 5 分钟，熔断期间直接走备用，不再请求主模型
 */
@Component
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Resource
    private SiliconFlowChatConfig siliconFlowConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // ========== 熔断状态 ==========
    private static final int FAILURE_THRESHOLD = 3;
    private static final int CIRCUIT_BREAK_SECONDS = 300; // 5 分钟

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant circuitOpenSince = null;

    /**
     * 调用主模型，失败时自动切换备用模型
     *
     * @param requestBody 请求体 JSON（含 messages, stream, temperature 等）
     * @return 主模型或备用模型的 Response（调用方负责 close）
     * @throws IOException 两个模型都失败时抛出
     */
    public Response call(JSONObject requestBody) throws IOException {
        // 1. 熔断检查 — 熔断中直接走备用
        if (isCircuitOpen()) {
            log.warn("【熔断】DeepSeek 熔断中，直接切换到 SiliconFlow");
            return callSiliconFlow(requestBody);
        }

        // 2. 尝试主模型 DeepSeek
        Request primaryRequest = buildDeepSeekRequest(requestBody);
        try {
            Response response = client.newCall(primaryRequest).execute();
            if (response.isSuccessful()) {
                // 成功 — 重置连续失败计数
                consecutiveFailures.set(0);
                if (circuitOpenSince != null) {
                    log.info("DeepSeek 恢复，关闭熔断");
                    circuitOpenSince = null;
                }
                return response;
            }

            // 主模型返回错误（5xx）— 关闭主响应，走备用
            response.close();
            log.warn("DeepSeek 返回错误码: {}, 切换到备用模型", response.code());
        } catch (IOException e) {
            log.warn("DeepSeek 请求失败: {}, 切换到备用模型", e.getMessage());
        }

        // 3. 记录失败，必要时开启熔断
        recordFailure();

        // 4. 调用备用模型
        return callSiliconFlow(requestBody);
    }

    /**
     * 构建 DeepSeek 请求（添加 model 字段）
     */
    private Request buildDeepSeekRequest(JSONObject originalBody) {
        JSONObject body = JSON.parseObject(originalBody.toJSONString());
        body.put("model", deepSeekConfig.getModel());
        return new Request.Builder()
                .url(deepSeekConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), body.toJSONString()))
                .build();
    }

    /**
     * 调用 SiliconFlow 备用模型（替换 model、url、key）
     */
    private Response callSiliconFlow(JSONObject originalBody) throws IOException {
        // 克隆 body，替换 model 字段
        JSONObject fallbackBody = JSON.parseObject(originalBody.toJSONString());
        fallbackBody.put("model", siliconFlowConfig.getModel());

        Request request = new Request.Builder()
                .url(siliconFlowConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + siliconFlowConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), fallbackBody.toJSONString()))
                .build();

        log.info("备用模型请求: model={}", siliconFlowConfig.getModel());
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            log.error("备用模型 SiliconFlow 也返回错误: code={}", response.code());
        }

        return response;
    }

    /**
     * 判断熔断是否开启
     */
    private boolean isCircuitOpen() {
        if (circuitOpenSince == null) {
            return false;
        }
        // 超过熔断时间 → 半开，重置失败计数，放行一次尝试
        if (Duration.between(circuitOpenSince, Instant.now()).getSeconds() > CIRCUIT_BREAK_SECONDS) {
            log.info("熔断时间已到，尝试恢复 DeepSeek");
            circuitOpenSince = null;
            consecutiveFailures.set(0);
            return false;
        }
        return true;
    }

    /**
     * 记录一次失败，达到阈值则开启熔断
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD && circuitOpenSince == null) {
            circuitOpenSince = Instant.now();
            log.warn("【熔断】DeepSeek 连续 {} 次失败，熔断 {} 秒",
                    FAILURE_THRESHOLD, CIRCUIT_BREAK_SECONDS);
        }
    }
}
