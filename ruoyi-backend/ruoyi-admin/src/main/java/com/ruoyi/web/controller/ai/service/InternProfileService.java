package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.web.controller.ai.domain.InternProfile;
import com.ruoyi.web.controller.ai.domain.InternProject;
import com.ruoyi.web.controller.ai.mapper.InternProfileMapper;
import com.ruoyi.web.controller.ai.mapper.InternProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 实习档案服务
 */
@Service
public class InternProfileService {

    @Resource
    private InternProfileMapper internProfileMapper;

    @Resource
    private InternProjectMapper internProjectMapper;

    /**
     * 获取用户档案（含项目经历）
     */
    public InternProfile getProfileByUserId(Long userId) {
        InternProfile profile = internProfileMapper.selectProfileByUserId(userId);
        if (profile != null) {
            List<InternProject> projects = internProjectMapper.selectProjectListByProfileId(profile.getId());
            profile.setProjects(projects);
        }
        return profile;
    }

    /**
     * 保存或更新档案（含项目经历）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId, Map<String, Object> data) {
        InternProfile profile = internProfileMapper.selectProfileByUserId(userId);

        if (profile == null) {
            profile = new InternProfile();
            profile.setUserId(userId);
            profile.setName((String) data.get("name"));
            profile.setSchool((String) data.get("school"));
            profile.setMajor((String) data.get("major"));
            profile.setEducation((String) data.get("education"));
            profile.setGraduationYear((String) data.get("graduationYear"));
            profile.setPhone((String) data.get("phone"));
            profile.setEmail((String) data.get("email"));
            profile.setSelfIntro((String) data.get("selfIntro"));
            profile.setDeleted(0);
            // 技能标签
            Object skillsObj = data.get("skills");
            if (skillsObj instanceof List) {
                profile.setSkills(JSON.toJSONString(skillsObj));
            } else if (skillsObj instanceof String) {
                profile.setSkills((String) skillsObj);
            }
            internProfileMapper.insertProfile(profile);
        } else {
            profile.setName((String) data.get("name"));
            profile.setSchool((String) data.get("school"));
            profile.setMajor((String) data.get("major"));
            profile.setEducation((String) data.get("education"));
            profile.setGraduationYear((String) data.get("graduationYear"));
            profile.setPhone((String) data.get("phone"));
            profile.setEmail((String) data.get("email"));
            profile.setSelfIntro((String) data.get("selfIntro"));
            Object skillsObj = data.get("skills");
            if (skillsObj instanceof List) {
                profile.setSkills(JSON.toJSONString(skillsObj));
            } else if (skillsObj instanceof String) {
                profile.setSkills((String) skillsObj);
            }
            internProfileMapper.updateProfile(profile);
        }

        // 处理项目经历
        Object projectsObj = data.get("projects");
        if (projectsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projectList = (List<Map<String, Object>>) projectsObj;
            // 先删除旧的项目经历
            internProjectMapper.deleteProjectByProfileId(profile.getId());
            // 插入新的
            int sort = 0;
            for (Map<String, Object> proj : projectList) {
                InternProject project = new InternProject();
                project.setProfileId(profile.getId());
                project.setProjectName((String) proj.get("projectName"));
                project.setStartDate((String) proj.get("startDate"));
                project.setEndDate((String) proj.get("endDate"));
                project.setRole((String) proj.get("role"));
                project.setDescription((String) proj.get("description"));
                project.setSortOrder(sort++);
                project.setDeleted(0);
                internProjectMapper.insertProject(project);
            }
        }
    }
}
