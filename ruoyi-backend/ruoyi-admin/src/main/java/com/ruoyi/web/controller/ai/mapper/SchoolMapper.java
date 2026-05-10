package com.ruoyi.web.controller.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.School;
import java.util.List;

/**
 * 学校 Mapper 接口
 */
@Mapper
public interface SchoolMapper {

    List<School> searchByKeyword(String keyword);

    List<School> searchByKeywordLimit(String keyword, Integer limit);
}
