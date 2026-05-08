package com.ruoyi.web.controller.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.InternProfile;

/**
 * 实习档案 Mapper 接口
 */
@Mapper
public interface InternProfileMapper {

    InternProfile selectProfileByUserId(Long userId);

    int insertProfile(InternProfile profile);

    int updateProfile(InternProfile profile);
}
