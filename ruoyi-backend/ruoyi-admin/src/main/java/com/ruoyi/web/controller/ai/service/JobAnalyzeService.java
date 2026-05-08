package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.domain.InternProfile;
import com.ruoyi.web.controller.ai.domain.InternProject;
import com.ruoyi.web.controller.ai.domain.JobPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 岗位 AI 匹配分析服务（Layer 2）
 * 调用 DeepSeek 对用户档案与岗位进行语义匹配评分
 */
@Service
public class JobAnalyzeService {

    private static final Logger log = LoggerFactory.getLogger(JobAnalyzeService.class);

    private static final String SYSTEM_PROMPT =
        "你是一位专业的求职匹配分析专家，服务于「江城聘」重庆综合招聘平台。你的任务是根据用户的个人档案和岗位信息，分析用户与该岗位的匹配程度。\n" +
        "\n" +
        "## 评分维度（每项 0-100 整数）\n" +
        "1. skills：技能匹配度 — 用户技能标签与岗位要求的关键词重合程度及技术栈互补性\n" +
        "2. education：学历匹配度 — 用户学历是否达到或超过岗位最低要求（博士>硕士>本科>大专）\n" +
        "3. experience：经验匹配度 — 用户毕业年份/项目经历年限与岗位经验要求的匹配程度\n" +
        "4. textRelevance：文本相关性 — 用户自我介绍、项目描述与岗位职责描述的语义相关程度\n" +
        "\n" +
        "## 评分规则\n" +
        "- 每项输出 0-100 的整数\n" +
        "- 若某维度数据缺失（如岗位未填学历要求），给 60 分（中性分）\n" +
        "- 最终 score = skills×35% + education×25% + experience×20% + textRelevance×20%，取整\n" +
        "- 因数据缺失导致维度得分全为 60 时，最终 score 应为 0（无法评估）\n" +
        "\n" +
        "## 输出要求\n" +
        "仅输出纯 JSON，不要包含 markdown 代码块标记或其他说明文字。reason 字段 50-100 字，包含匹配亮点和潜在不足。\n" +
        "示例：\n" +
        "{\"score\":85,\"reason\":\"用户技能与岗位高度匹配，Java和Spring Boot经验丰富，学历满足要求。但缺少微服务相关项目经验。\",\"dimensions\":{\"skills\":90,\"education\":100,\"experience\":70,\"textRelevance\":80}}";

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private InternProfileService internProfileService;

    @Resource
    private JobPostingService jobPostingService;

    /**
     * 分析岗位匹配度
     *
     * @param jobId  岗位ID
     * @param userId 当前用户ID
     * @return 匹配分析结果
     */
    public Map<String, Object> analyze(Long jobId, Long userId) {
        // 1. 获取岗位
        JobPosting job = jobPostingService.selectJobById(jobId);
        if (job == null) {
            return errorResult("岗位不存在或已删除");
        }

        // 2. 获取用户档案
        InternProfile profile = internProfileService.getProfileByUserId(userId);
        if (profile == null) {
            return errorResult("请先填写个人档案");
        }

        // 3. 构建 prompt
        String prompt = buildPrompt(profile, job);

        // 4. 调用 DeepSeek
        String response;
        try {
            JSONArray messages = deepSeekService.buildMessages(SYSTEM_PROMPT, null, prompt);
            response = deepSeekService.callDeepSeek(messages);
            log.info("AI分析完成: jobId={}, responseLen={}", jobId, response.length());
        } catch (Exception e) {
            log.error("AI分析调用失败: jobId={}", jobId, e);
            return errorResult("AI 服务暂时不可用，请稍后重试");
        }

        // 5. 解析响应
        return parseAnalysis(response);
    }

    /**
     * 构建分析 prompt
     */
    private String buildPrompt(InternProfile profile, JobPosting job) {
        StringBuilder sb = new StringBuilder();

        // 用户画像
        sb.append("## 用户画像\n");
        sb.append("- 姓名：").append(profile.getName() != null ? profile.getName() : "未填写").append("\n");
        sb.append("- 学校：").append(profile.getSchool() != null ? profile.getSchool() : "未填写").append("\n");
        sb.append("- 专业：").append(profile.getMajor() != null ? profile.getMajor() : "未填写").append("\n");
        sb.append("- 学历：").append(profile.getEducation() != null ? profile.getEducation() : "未填写").append("\n");
        sb.append("- 毕业年份：").append(profile.getGraduationYear() != null ? profile.getGraduationYear() : "未填写").append("\n");

        List<String> skills = profile.getSkillsList();
        if (!skills.isEmpty()) {
            sb.append("- 技能标签：").append(String.join("、", skills)).append("\n");
        }

        if (profile.getSelfIntro() != null && !profile.getSelfIntro().isEmpty()) {
            sb.append("- 个人简介：").append(profile.getSelfIntro()).append("\n");
        }

        List<InternProject> projects = profile.getProjects();
        if (projects != null && !projects.isEmpty()) {
            sb.append("- 项目经历：\n");
            for (int i = 0; i < projects.size(); i++) {
                InternProject p = projects.get(i);
                sb.append("  ").append(i + 1).append(". ");
                if (p.getProjectName() != null) sb.append(p.getProjectName());
                if (p.getRole() != null) sb.append("（").append(p.getRole()).append("）");
                sb.append("\n");
                if (p.getDescription() != null) sb.append("    ").append(p.getDescription()).append("\n");
            }
        }

        // 岗位信息
        sb.append("\n## 岗位信息\n");
        sb.append("- 职位名称：").append(job.getTitle() != null ? job.getTitle() : "").append("\n");
        sb.append("- 公司：").append(job.getCompany() != null ? job.getCompany() : "").append("\n");
        sb.append("- 薪资：");
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            sb.append(job.getSalaryMin()).append("-").append(job.getSalaryMax()).append("元/月\n");
        } else {
            sb.append("面议\n");
        }
        sb.append("- 地点：").append(job.getLocation() != null ? job.getLocation() : "").append("\n");
        sb.append("- 学历要求：").append(job.getEducation() != null ? job.getEducation() : "不限").append("\n");
        sb.append("- 经验要求：").append(job.getExperience() != null ? job.getExperience() : "不限").append("\n");

        if (job.getTags() != null && !job.getTags().isEmpty()) {
            try {
                List<String> tags = JSON.parseArray(job.getTags(), String.class);
                sb.append("- 技能标签：").append(String.join("、", tags)).append("\n");
            } catch (Exception ignored) {}
        }

        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            sb.append("- 岗位职责：").append(job.getDescription()).append("\n");
        }

        if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
            sb.append("- 招聘要求：").append(job.getRequirements()).append("\n");
        }

        sb.append("\n请分析用户与岗位的匹配度，仅返回JSON格式。");

        return sb.toString();
    }

    /**
     * 解析 AI 返回的 JSON 结果
     */
    private Map<String, Object> parseAnalysis(String response) {
        // 提取 JSON（AI 可能返回 markdown 包裹）
        String json = response;
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end > start) {
            json = response.substring(start, end + 1);
        }

        try {
            JSONObject obj = JSON.parseObject(json);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("score", obj.getIntValue("score", 0));
            result.put("reason", obj.getString("reason"));

            JSONObject dims = obj.getJSONObject("dimensions");
            if (dims != null) {
                result.put("dimensions", dims);
            }

            return result;
        } catch (Exception e) {
            log.warn("AI 响应解析失败: {}", response);
            return errorResult("AI 分析结果解析失败");
        }
    }

    /**
     * 构造错误结果
     */
    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", 0);
        result.put("reason", message);
        return result;
    }
}
