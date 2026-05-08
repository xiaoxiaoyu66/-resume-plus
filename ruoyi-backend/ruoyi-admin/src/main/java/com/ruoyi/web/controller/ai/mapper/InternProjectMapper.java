package com.ruoyi.web.controller.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.InternProject;

/**
 * 项目经历 Mapper 接口
 */
@Mapper
public interface InternProjectMapper {

    List<InternProject> selectProjectListByProfileId(Long profileId);

    int insertProject(InternProject project);

    int updateProject(InternProject project);

    int deleteProjectByProfileId(Long profileId);

    int deleteProjectById(Long id);
}
