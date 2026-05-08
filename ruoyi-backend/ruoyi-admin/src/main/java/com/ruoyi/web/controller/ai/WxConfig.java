package com.ruoyi.web.controller.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 微信开放平台配置
 */
@Configuration
@PropertySource("classpath:wx.properties")
@ConfigurationProperties(prefix = "wx")
public class WxConfig {

    /** 微信 AppId */
    private String appId;

    /** 微信 AppSecret */
    private String appSecret;

    /** OAuth2 授权回调地址 */
    private String redirectUri;

    /** 二维码有效期（秒） */
    private int qrExpireSeconds = 300;

    // ---- Getters & Setters ----

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public int getQrExpireSeconds() { return qrExpireSeconds; }
    public void setQrExpireSeconds(int qrExpireSeconds) { this.qrExpireSeconds = qrExpireSeconds; }
}
