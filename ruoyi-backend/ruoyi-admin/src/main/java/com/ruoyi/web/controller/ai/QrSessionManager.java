package com.ruoyi.web.controller.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 二维码会话管理器
 * 使用 Redis 管理微信扫码登录的会话状态
 * 
 * 流程:
 *   1. 前端请求生成二维码 -> 创建 session(storing sceneStr+status=pending) + 返回二维码URL
 *   2. 用户用微信扫码 -> 微信回调我们的 callback 接口 -> 更新状态为 scanned(存入openid)
 *   3. 前端轮询 /login/wechat/poll?sceneStr=xxx -> 如果状态变为 scanned，返回 token
 *   4. 用户在前端确认登录 -> (可选) 或直接返回 token 完成登录
 */
@Component
public class QrSessionManager {

    private static final Logger log = LoggerFactory.getLogger(QrSessionManager.class);

    /** Redis 中 QR 会话的前缀 */
    private static final String QR_SESSION_PREFIX = "qr_session:";

    /** Redis 中 scene_str -> sessionId 映射 */
    private static final String QR_SCENE_PREFIX = "qr_scene:";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 创建一个新的二维码会话
     *
     * @param expireSeconds 会话过期时间（秒）
     * @return 会话信息 (sessionId, sceneStr)
     */
    public JSONObject createSession(int expireSeconds) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String sceneStr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 存储 session 信息
        JSONObject session = new JSONObject();
        session.put("sessionId", sessionId);
        session.put("sceneStr", sceneStr);
        session.put("status", "pending");
        session.put("createTime", System.currentTimeMillis());

        // 存入 Redis
        redisTemplate.opsForValue().set(
                QR_SESSION_PREFIX + sessionId,
                session.toJSONString(),
                expireSeconds,
                TimeUnit.SECONDS
        );

        // scene_str -> sessionId 映射（用于微信回调时查找）
        redisTemplate.opsForValue().set(
                QR_SCENE_PREFIX + sceneStr,
                sessionId,
                expireSeconds,
                TimeUnit.SECONDS
        );

        log.info("创建二维码会话: sessionId={}, sceneStr={}", sessionId, sceneStr);
        return session;
    }

    /**
     * 用户扫码后更新会话状态（由微信回调触发）
     *
     * @param sceneStr 二维码场景值
     * @param openid   用户微信 openid
     * @param wxUserInfo 微信用户信息 (JSON string)
     * @return true 如果更新成功
     */
    public boolean onScanned(String sceneStr, String openid, String wxUserInfo) {
        // 通过 scene_str 查找 sessionId
        String sessionId = redisTemplate.opsForValue().get(QR_SCENE_PREFIX + sceneStr);
        if (sessionId == null) {
            log.warn("二维码会话已过期: sceneStr={}", sceneStr);
            return false;
        }

        String sessionJson = redisTemplate.opsForValue().get(QR_SESSION_PREFIX + sessionId);
        if (sessionJson == null) {
            log.warn("二维码会话不存在或已过期: sessionId={}", sessionId);
            return false;
        }

        JSONObject session = JSON.parseObject(sessionJson);
        session.put("status", "scanned");
        session.put("openid", openid);
        if (wxUserInfo != null) {
            session.put("wxUserInfo", wxUserInfo);
        }
        session.put("scanTime", System.currentTimeMillis());

        // 获取剩余 TTL
        Long ttl = redisTemplate.getExpire(QR_SESSION_PREFIX + sessionId, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return false;
        }

        redisTemplate.opsForValue().set(
                QR_SESSION_PREFIX + sessionId,
                session.toJSONString(),
                ttl,
                TimeUnit.SECONDS
        );

        log.info("用户已扫码: sessionId={}, openid={}", sessionId, openid);
        return true;
    }

    /**
     * 确认登录（用户确认授权后调用）
     *
     * @param sessionId 会话ID
     * @param token     生成的登录 token
     */
    public void confirmLogin(String sessionId, String token) {
        String sessionJson = redisTemplate.opsForValue().get(QR_SESSION_PREFIX + sessionId);
        if (sessionJson == null) {
            return;
        }

        JSONObject session = JSON.parseObject(sessionJson);
        session.put("status", "confirmed");
        session.put("token", token);
        session.put("confirmTime", System.currentTimeMillis());

        // 获取剩余 TTL
        Long ttl = redisTemplate.getExpire(QR_SESSION_PREFIX + sessionId, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) return;

        redisTemplate.opsForValue().set(
                QR_SESSION_PREFIX + sessionId,
                session.toJSONString(),
                ttl,
                TimeUnit.SECONDS
        );

        log.info("登录已确认: sessionId={}", sessionId);
    }

    /**
     * 轮询二维码状态
     *
     * @param sessionId 会话ID
     * @return 当前会话状态 JSON (status, token 等)
     */
    public JSONObject pollStatus(String sessionId) {
        String sessionJson = redisTemplate.opsForValue().get(QR_SESSION_PREFIX + sessionId);
        if (sessionJson == null) {
            JSONObject expired = new JSONObject();
            expired.put("status", "expired");
            expired.put("message", "二维码已过期");
            return expired;
        }

        JSONObject session = JSON.parseObject(sessionJson);
        JSONObject result = new JSONObject();
        result.put("status", session.getString("status"));

        if ("confirmed".equals(session.getString("status"))) {
            result.put("token", session.getString("token"));
        }

        return result;
    }

    /**
     * 通过 scene_str 获取 sessionId
     */
    public String getSessionIdBySceneStr(String sceneStr) {
        return redisTemplate.opsForValue().get(QR_SCENE_PREFIX + sceneStr);
    }

    /**
     * 通过 sessionId 获取会话
     */
    public JSONObject getSession(String sessionId) {
        String sessionJson = redisTemplate.opsForValue().get(QR_SESSION_PREFIX + sessionId);
        if (sessionJson == null) return null;
        return JSON.parseObject(sessionJson);
    }

    /**
     * 标记会话为已过期
     */
    public void expireSession(String sessionId) {
        String sessionJson = redisTemplate.opsForValue().get(QR_SESSION_PREFIX + sessionId);
        if (sessionJson != null) {
            JSONObject session = JSON.parseObject(sessionJson);
            session.put("status", "expired");
            redisTemplate.opsForValue().set(
                    QR_SESSION_PREFIX + sessionId,
                    session.toJSONString(),
                    60,
                    TimeUnit.SECONDS
            );
        }
    }
}
