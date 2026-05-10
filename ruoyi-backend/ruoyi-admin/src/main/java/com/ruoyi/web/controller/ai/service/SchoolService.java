package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.domain.School;
import com.ruoyi.web.controller.ai.mapper.SchoolMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 学校搜索服务
 */
@Service
public class SchoolService {

    @Resource
    private SchoolMapper schoolMapper;

    /**
     * 按关键词搜索学校，最多返回 limit 条
     */
    public List<School> searchSchools(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        // 限制最大 20 条，防止滥用
        if (limit <= 0 || limit > 20) {
            limit = 5;
        }
        return schoolMapper.searchByKeywordLimit(keyword.trim(), limit);
    }
}
