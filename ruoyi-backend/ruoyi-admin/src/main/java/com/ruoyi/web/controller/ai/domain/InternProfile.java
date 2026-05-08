package com.ruoyi.web.controller.ai.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.fastjson2.JSON;

/**
 * 实习档案对象 intern_profile
 */
public class InternProfile {

    /** 档案ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 姓名 */
    private String name;

    /** 学校 */
    private String school;

    /** 专业 */
    private String major;

    /** 学历 */
    private String education;

    /** 毕业年份 */
    private String graduationYear;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 个人简介 */
    private String selfIntro;

    /** 技能标签（JSON数组） */
    private String skills;

    /** 项目经历列表 */
    private List<InternProject> projects;

    /** 删除标志（0-正常 1-删除） */
    private Integer deleted;

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getGraduationYear() { return graduationYear; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSelfIntro() { return selfIntro; }
    public void setSelfIntro(String selfIntro) { this.selfIntro = selfIntro; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    /**
     * 获取技能标签数组（用于前端展示）
     */
    public List<String> getSkillsList() {
        if (skills == null || skills.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(skills, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<InternProject> getProjects() { return projects; }
    public void setProjects(List<InternProject> projects) { this.projects = projects; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
