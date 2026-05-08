package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.config.SmsConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务
 * 支持测试模式：验证码直接打印到日志，不真正发送短信
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Resource
    private SmsConfig smsConfig;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 测试模式开关 - 设为true时验证码直接打印到控制台，不发送真实短信
    private static final boolean TEST_MODE = true;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        if (TEST_MODE) {
            log.info("========================================");
            log.info("短信服务运行中：测试模式");
            log.info("验证码将直接打印在日志中，不会发送真实短信");
            log.info("========================================");
        } else {
            log.info("短信服务运行中：生产模式");
        }
    }

    /**
     * 生成6位随机验证码
     */
    public String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码（如果为null则自动生成）
     * @return true=发送成功, false=发送失败
     */
    public boolean sendSmsCode(String phone, String code) {
        try {
            // 如果没有传入code，生成一个
            if (code == null) {
                code = generateCode();
            }

            // 将验证码存入Redis，5分钟有效期
            String redisKey = "sms:code:" + phone;
            redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

            if (TEST_MODE) {
                // 测试模式：直接打印验证码到日志
                log.info("========================================");
                log.info("【测试模式】短信验证码");
                log.info("手机号：{}", phone);
                log.info("验证码：{}", code);
                log.info("有效期：5分钟");
                log.info("========================================");
                return true;
            } else {
                // 生产模式：调用阿里云短信服务
                return sendRealSms(phone, code);
            }
        } catch (Exception e) {
            log.error("短信发送异常 -> phone: {}", phone, e);
            return false;
        }
    }

    /**
     * 验证短信验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=验证成功, false=验证失败
     */
    public boolean verifyCode(String phone, String code) {
        try {
            String redisKey = "sms:code:" + phone;
            String storedCode = redisTemplate.opsForValue().get(redisKey);

            if (storedCode == null) {
                log.warn("验证码已过期或不存在 -> phone: {}", phone);
                return false;
            }

            if (storedCode.equals(code)) {
                // 验证成功后删除验证码
                redisTemplate.delete(redisKey);
                return true;
            } else {
                log.warn("验证码不匹配 -> phone: {}, input: {}, expected: {}", phone, code, storedCode);
                return false;
            }
        } catch (Exception e) {
            log.error("验证码验证异常 -> phone: {}", phone, e);
            return false;
        }
    }

    /**
     * 发送真实短信（生产模式使用）
     */
    private boolean sendRealSms(String phone, String code) {
        try {
            // 这里可以接入真实的阿里云短信服务
            // 目前先返回成功，方便测试
            log.info("生产模式短信发送 -> phone: {}, code: {}", phone, code);
            return true;
        } catch (Exception e) {
            log.error("真实短信发送失败", e);
            return false;
        }
    }
}
