package com.ruoyi.web.controller.ai.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.fastjson2.JSON;
import java.time.LocalDateTime;

/**
 * 简历对象 resume
 */
public class Resume {

    /** 简历ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 模板ID：classic/modern/minimal */
    private String templateId;

    /** 简历标题 */
    private String title;

    /** 完整简历内容（JSON字符串） */
    private String content;

    /** 是否默认简历 */
    private Integer isDefault;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
