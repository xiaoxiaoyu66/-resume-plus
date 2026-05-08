package com.ruoyi.web.controller.ai.mapper;

import com.ruoyi.web.controller.ai.domain.Resume;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 简历Mapper接口
 */
public interface ResumeMapper {

    /**
     * 根据ID查询简历
     */
    Resume selectResumeById(Long id);

    /**
     * 查询用户的简历列表
     */
    List<Resume> selectResumeListByUserId(@Param("userId") Long userId);

    /**
     * 插入简历
     */
    int insertResume(Resume resume);

    /**
     * 更新简历
     */
    int updateResume(Resume resume);

    /**
     * 删除简历
     */
    int deleteResumeById(@Param("id") Long id);

    /**
     * 取消用户所有默认简历
     */
    int cancelDefaultByUserId(@Param("userId") Long userId);
}
