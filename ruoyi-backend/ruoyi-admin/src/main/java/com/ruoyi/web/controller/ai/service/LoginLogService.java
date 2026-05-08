package com.ruoyi.web.controller.ai.service;

import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.web.controller.ai.domain.LoginLog;
import com.ruoyi.web.controller.ai.mapper.LoginLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务
 *
 * @author ruoyi
 * @date 2026-04-25
 */
@Service
public class LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);

    @Resource
    private LoginLogMapper loginLogMapper;

    /**
     * 记录登录日志
     *
     * @param userId      用户ID
     * @param userName    用户账号
     * @param loginType   登录类型
     * @param request     HTTP请求
     * @param loginStatus 登录状态：0-成功, 1-失败
     * @param msg         提示消息
     */
    public void recordLoginLog(Long userId, String userName, String loginType,
                               HttpServletRequest request, String loginStatus, String msg) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setUserName(userName);
            loginLog.setLoginType(loginType);
            loginLog.setLoginStatus(loginStatus);
            loginLog.setMsg(msg);

            // 获取IP地址
            String ip = IpUtils.getIpAddr(request);
            loginLog.setIpAddress(ip);

            // 登录地点（简化处理）
            loginLog.setLoginLocation("未知");

            // 解析User-Agent（简化处理）
            String userAgentStr = request.getHeader("User-Agent");
            if (userAgentStr != null && !userAgentStr.isEmpty()) {
                // 简单解析浏览器
                if (userAgentStr.contains("Chrome")) {
                    loginLog.setBrowser("Chrome");
                } else if (userAgentStr.contains("Firefox")) {
                    loginLog.setBrowser("Firefox");
                } else if (userAgentStr.contains("Safari")) {
                    loginLog.setBrowser("Safari");
                } else if (userAgentStr.contains("Edge")) {
                    loginLog.setBrowser("Edge");
                } else {
                    loginLog.setBrowser("Other");
                }

                // 简单解析操作系统
                if (userAgentStr.contains("Windows")) {
                    loginLog.setOs("Windows");
                } else if (userAgentStr.contains("Mac")) {
                    loginLog.setOs("Mac OS");
                } else if (userAgentStr.contains("Linux")) {
                    loginLog.setOs("Linux");
                } else if (userAgentStr.contains("Android")) {
                    loginLog.setOs("Android");
                } else if (userAgentStr.contains("iPhone") || userAgentStr.contains("iPad")) {
                    loginLog.setOs("iOS");
                } else {
                    loginLog.setOs("Other");
                }
            } else {
                loginLog.setBrowser("未知");
                loginLog.setOs("未知");
            }

            // 保存到数据库
            loginLogMapper.insertLoginLog(loginLog);
            log.debug("登录日志记录成功：userName={}, loginType={}", userName, loginType);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    /**
     * 记录成功登录日志
     */
    public void recordSuccessLogin(Long userId, String userName, String loginType,
                                   HttpServletRequest request) {
        recordLoginLog(userId, userName, loginType, request, "0", "登录成功");
    }

    /**
     * 记录失败登录日志
     */
    public void recordFailLogin(String userName, String loginType,
                                HttpServletRequest request, String failMsg) {
        recordLoginLog(null, userName, loginType, request, "1", failMsg);
    }
}
