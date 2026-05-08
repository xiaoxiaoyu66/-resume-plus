package com.ruoyi.web.controller.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.controller.ai.service.LoginLogService;
import com.ruoyi.web.controller.ai.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 模块登录扩展（手机验证码 + 微信扫码登录）
 * 
 * 微信扫码登录使用模拟实现，真实微信API需要配置有效的appId和appSecret
 */
@RestController
@RequestMapping("/login")
public class AiLoginController {

    private static final Logger log = LoggerFactory.getLogger(AiLoginController.class);

    @Resource
    private SysLoginService loginService;

    @Resource
    private TokenService tokenService;

    @Resource
    private ISysUserService userService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private QrSessionManager qrSessionManager;

    @Resource
    private WxService wxService;

    @Resource
    private WxConfig wxConfig;

    @Resource
    private SmsService smsService;

    @Resource
    private LoginLogService loginLogService;

    private static final String SMS_CODE_PREFIX = "sms_code:";

    // ==================== 手机验证码登录 ====================

    /**
     * 发送短信验证码
     * POST /login/sendSms
     */
    @PostMapping("/sendSms")
    public AjaxResult sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (StringUtils.isEmpty(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            return AjaxResult.error("手机号格式不正确");
        }

        // 生成6位随机验证码
        String code = smsService.generateCode();

        // 调用阿里云短信发送
        boolean sent = smsService.sendSmsCode(phone, code);
        if (!sent) {
            return AjaxResult.error("短信发送失败，请稍后重试");
        }

        // 存储到 Redis，有效期5分钟
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);

        return AjaxResult.success("验证码已发送");
    }

    /**
     * 手机验证码登录
     * POST /login/phone
     */
    @PostMapping("/phone")
    public AjaxResult phoneLogin(@RequestBody PhoneLoginBody body, HttpServletRequest request) {
        String phone = body.getPhone();
        String code = body.getCode();

        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            loginLogService.recordFailLogin(phone, "phone", request, "手机号和验证码不能为空");
            return AjaxResult.error("手机号和验证码不能为空");
        }

        // 验证验证码
        String cacheCode = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (StringUtils.isEmpty(cacheCode)) {
            loginLogService.recordFailLogin(phone, "phone", request, "验证码已过期");
            return AjaxResult.error("验证码已过期，请重新发送");
        }
        if (!cacheCode.equals(code)) {
            loginLogService.recordFailLogin(phone, "phone", request, "验证码错误");
            return AjaxResult.error("验证码错误");
        }

        // 删除已使用的验证码
        redisTemplate.delete(SMS_CODE_PREFIX + phone);

        // 通过手机号查找用户，不存在则自动注册
        SysUser user = userService.selectUserByUserName(phone);
        boolean isNewUser = false;
        if (user == null) {
            // 自动注册新用户
            isNewUser = true;
            user = new SysUser();
            user.setUserName(phone);
            user.setNickName("用户" + phone.substring(phone.length() - 4));
            user.setPhonenumber(phone);
            user.setPassword(SecurityUtils.encryptPassword("123456"));
            userService.registerUser(user);
            // 重新查询获取完整信息
            user = userService.selectUserByUserName(phone);
        }

        if (user == null) {
            loginLogService.recordFailLogin(phone, "phone", request, "用户注册失败");
            return AjaxResult.error("用户注册失败");
        }

        // 生成 token
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        loginUser.setUserId(user.getUserId());
        String token = tokenService.createToken(loginUser);

        // 记录登录成功日志
        String msg = isNewUser ? "登录成功（新用户自动注册）" : "登录成功";
        loginLogService.recordSuccessLogin(user.getUserId(), phone, "phone", request);

        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    // ==================== 微信扫码登录（模拟实现） ====================

    /**
     * 获取微信扫码二维码（模拟实现）
     * GET /login/wechat/qrcode
     */
    @GetMapping("/wechat/qrcode")
    public AjaxResult getWechatQrCode() {
        try {
            // 创建二维码会话
            JSONObject session = qrSessionManager.createSession(wxConfig.getQrExpireSeconds());
            String sessionId = session.getString("sessionId");

            // 构建模拟的 OAuth2 授权 URL
            String oauthUrl = wxService.buildOAuthUrl(sessionId);

            AjaxResult ajax = AjaxResult.success();
            ajax.put("sessionId", sessionId);
            ajax.put("qrUrl", oauthUrl);
            ajax.put("expireSeconds", wxConfig.getQrExpireSeconds());
            return ajax;
        } catch (Exception e) {
            log.error("生成微信二维码失败", e);
            return AjaxResult.error("生成二维码失败: " + e.getMessage());
        }
    }

    /**
     * 微信 OAuth2 回调接口（模拟实现）
     * GET /login/wechat/callback?code=xxx&state=xxx
     */
    @GetMapping("/wechat/callback")
    public AjaxResult wechatCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        log.info("微信回调: code={}, state={}", code, state);

        // 模拟实现：直接返回成功，不调用真实微信API
        // 实际生产环境需要配置有效的微信开放平台应用
        return AjaxResult.success("微信回调已接收（模拟实现）");
    }

    /**
     * 轮询二维码状态
     * GET /login/wechat/poll?sessionId=xxx
     */
    @GetMapping("/wechat/poll")
    public AjaxResult pollWechatStatus(@RequestParam("sessionId") String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return AjaxResult.error("sessionId 不能为空");
        }

        JSONObject status = qrSessionManager.pollStatus(sessionId);
        String statusStr = status.getString("status");

        AjaxResult ajax = AjaxResult.success();
        ajax.put("status", statusStr);

        switch (statusStr) {
            case "pending":
                ajax.put("message", "等待扫码");
                break;
            case "scanned":
                ajax.put("message", "已扫码，正在确认登录");
                break;
            case "confirmed":
                ajax.put(Constants.TOKEN, status.getString("token"));
                ajax.put("message", "登录成功");
                break;
            case "expired":
                ajax.put("message", "二维码已过期，请刷新");
                break;
            default:
                ajax.put("message", "未知状态");
        }

        return ajax;
    }

    // ==================== 请求体类 ====================

    /**
     * 手机验证码登录请求体
     */
    public static class PhoneLoginBody {
        private String phone;
        private String code;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
