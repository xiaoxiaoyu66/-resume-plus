package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.domain.Resume;

import java.util.List;

/**
 * 简历Service接口
 */
public interface IResumeService {

    /**
     * 根据ID查询简历
     */
    Resume selectResumeById(Long id);

    /**
     * 查询当前用户的简历列表
     */
    List<Resume> selectCurrentUserResumeList();

    /**
     * 新增简历
     */
    int insertResume(Resume resume);

    /**
     * 更新简历
     */
    int updateResume(Resume resume);

    /**
     * 删除简历
     */
    int deleteResume(Long id);

    /**
     * 设置默认简历
     */
    int setDefaultResume(Long id);
}
