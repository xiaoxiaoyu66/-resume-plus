package com.ruoyi.web.controller.ai.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.domain.Resume;
import com.ruoyi.web.controller.ai.mapper.ResumeMapper;
import com.ruoyi.web.controller.ai.service.IResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 简历Service实现
 */
@Service
public class ResumeServiceImpl implements IResumeService {

    @Autowired
    private ResumeMapper resumeMapper;

    @Override
    public Resume selectResumeById(Long id) {
        Resume resume = resumeMapper.selectResumeById(id);
        // 权限校验：只能查看自己的简历
        if (resume != null && !resume.getUserId().equals(SecurityUtils.getUserId())) {
            return null;
        }
        return resume;
    }

    @Override
    public List<Resume> selectCurrentUserResumeList() {
        return resumeMapper.selectResumeListByUserId(SecurityUtils.getUserId());
    }

    @Override
    public int insertResume(Resume resume) {
        resume.setUserId(SecurityUtils.getUserId());
        if (resume.getTemplateId() == null) {
            resume.setTemplateId("modern");
        }
        if (resume.getIsDefault() == null) {
            resume.setIsDefault(0);
        }
        return resumeMapper.insertResume(resume);
    }

    @Override
    public int updateResume(Resume resume) {
        // 权限校验
        Resume exist = resumeMapper.selectResumeById(resume.getId());
        if (exist == null || !exist.getUserId().equals(SecurityUtils.getUserId())) {
            throw new RuntimeException("无权修改此简历");
        }
        return resumeMapper.updateResume(resume);
    }

    @Override
    public int deleteResume(Long id) {
        // 权限校验
        Resume exist = resumeMapper.selectResumeById(id);
        if (exist == null || !exist.getUserId().equals(SecurityUtils.getUserId())) {
            throw new RuntimeException("无权删除此简历");
        }
        return resumeMapper.deleteResumeById(id);
    }

    @Override
    public int setDefaultResume(Long id) {
        Resume exist = resumeMapper.selectResumeById(id);
        if (exist == null || !exist.getUserId().equals(SecurityUtils.getUserId())) {
            throw new RuntimeException("无权操作此简历");
        }
        resumeMapper.cancelDefaultByUserId(SecurityUtils.getUserId());
        Resume update = new Resume();
        update.setId(id);
        update.setIsDefault(1);
        return resumeMapper.updateResume(update);
    }
}
