-- 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    log_id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '日志ID',
    user_id         BIGINT(20)      DEFAULT NULL               COMMENT '用户ID',
    user_name       VARCHAR(50)     DEFAULT NULL               COMMENT '用户账号',
    login_type      VARCHAR(20)     DEFAULT 'password'         COMMENT '登录类型：password-密码登录, phone-手机验证码, wechat-微信登录',
    ip_address      VARCHAR(128)    DEFAULT NULL               COMMENT '登录IP地址',
    login_location  VARCHAR(255)    DEFAULT NULL               COMMENT '登录地点',
    browser         VARCHAR(50)     DEFAULT NULL               COMMENT '浏览器类型',
    os              VARCHAR(50)     DEFAULT NULL               COMMENT '操作系统',
    login_status    CHAR(1)         DEFAULT '0'                COMMENT '登录状态：0-成功, 1-失败',
    msg             VARCHAR(255)    DEFAULT NULL               COMMENT '提示消息',
    login_time      DATETIME        DEFAULT CURRENT_TIMESTAMP  COMMENT '登录时间',
    PRIMARY KEY (log_id),
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_login_type (login_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统登录日志表';
