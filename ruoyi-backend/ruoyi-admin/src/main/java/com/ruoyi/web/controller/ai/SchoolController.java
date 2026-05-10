package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.controller.ai.domain.School;
import com.ruoyi.web.controller.ai.service.SchoolService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 学校搜索控制器
 */
@RestController
@RequestMapping("/ai/schools")
public class SchoolController {

    @Resource
    private SchoolService schoolService;

    /**
     * 搜索学校
     * @param keyword 关键词
     * @param limit   返回条数（默认 5，最大 20）
     */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String keyword,
                             @RequestParam(defaultValue = "5") int limit) {
        List<School> list = schoolService.searchSchools(keyword, limit);
        return AjaxResult.success(list);
    }
}
