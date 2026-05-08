package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.web.controller.ai.domain.InternProfile;
import com.ruoyi.web.controller.ai.domain.InternProject;
import com.ruoyi.web.controller.ai.domain.JobPosting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 岗位 AI 匹配分析服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class JobAnalyzeServiceTest {

    @Mock
    private DeepSeekService deepSeekService;

    @Mock
    private InternProfileService internProfileService;

    @Mock
    private JobPostingService jobPostingService;

    @InjectMocks
    private JobAnalyzeService jobAnalyzeService;

    @Test
    void analyze_shouldReturnError_whenJobNotFound() {
        when(jobPostingService.selectJobById(999L)).thenReturn(null);

        Map<String, Object> result = jobAnalyzeService.analyze(999L, 1L);

        assertEquals(0, result.get("score"));
        assertEquals("岗位不存在或已删除", result.get("reason"));
        verify(jobPostingService).selectJobById(999L);
        verifyNoInteractions(internProfileService, deepSeekService);
    }

    @Test
    void analyze_shouldReturnError_whenProfileNotFound() {
        JobPosting job = new JobPosting();
        job.setId(1L);
        job.setTitle("Java开发工程师");
        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(null);

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(0, result.get("score"));
        assertEquals("请先填写个人档案", result.get("reason"));
    }

    @Test
    void analyze_shouldReturnError_whenDeepSeekFails() {
        JobPosting job = new JobPosting();
        job.setId(1L);
        job.setTitle("Java开发工程师");
        InternProfile profile = createSampleProfile();

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("API timeout"));

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(0, result.get("score"));
        assertEquals("AI 服务暂时不可用，请稍后重试", result.get("reason"));
    }

    @Test
    void analyze_shouldParseValidJsonResponse() {
        JobPosting job = createSampleJob();
        InternProfile profile = createSampleProfile();
        String aiResponse = "{\"score\":85,\"reason\":\"技能匹配度高\",\"dimensions\":{\"skills\":90,\"education\":100,\"experience\":70,\"textRelevance\":80}}";

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), isNull(), anyString()))
                .thenReturn(new JSONArray());
        when(deepSeekService.callDeepSeek(any())).thenReturn(aiResponse);

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(85, result.get("score"));
        assertEquals("技能匹配度高", result.get("reason"));
        assertNotNull(result.get("dimensions"));
        assertInstanceOf(Map.class, result.get("dimensions"));
    }

    @Test
    void analyze_shouldHandleMarkdownCodeBlockResponse() {
        JobPosting job = createSampleJob();
        InternProfile profile = createSampleProfile();
        // Simulate AI returning markdown-wrapped JSON
        String aiResponse = "```json\n{\"score\":72,\"reason\":\"匹配一般\",\"dimensions\":{\"skills\":80,\"education\":60,\"experience\":50,\"textRelevance\":70}}\n```";

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), isNull(), anyString()))
                .thenReturn(new JSONArray());
        when(deepSeekService.callDeepSeek(any())).thenReturn(aiResponse);

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(72, result.get("score"));
        assertEquals("匹配一般", result.get("reason"));
    }

    @Test
    void analyze_shouldHandleInvalidJsonResponse() {
        JobPosting job = createSampleJob();
        InternProfile profile = createSampleProfile();

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), isNull(), anyString()))
                .thenReturn(new JSONArray());
        when(deepSeekService.callDeepSeek(any())).thenReturn("not json at all");

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(0, result.get("score"));
        assertEquals("AI 分析结果解析失败", result.get("reason"));
    }

    @Test
    void analyze_shouldHandlePartialDimensionsResponse() {
        JobPosting job = createSampleJob();
        InternProfile profile = createSampleProfile();
        // Missing 'dimensions' field
        String aiResponse = "{\"score\":60,\"reason\":\"数据不足\"}";

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), isNull(), anyString()))
                .thenReturn(new JSONArray());
        when(deepSeekService.callDeepSeek(any())).thenReturn(aiResponse);

        Map<String, Object> result = jobAnalyzeService.analyze(1L, 1L);

        assertEquals(60, result.get("score"));
        assertNull(result.get("dimensions"));
    }

    @Test
    void analyze_shouldBuildPromptWithProfileAndJobData() {
        JobPosting job = createSampleJob();
        job.setTags("[\"Java\",\"Spring Boot\",\"MySQL\"]");
        job.setDescription("负责后端开发");
        job.setRequirements("3年以上经验");
        InternProfile profile = createSampleProfile();

        when(jobPostingService.selectJobById(1L)).thenReturn(job);
        when(internProfileService.getProfileByUserId(1L)).thenReturn(profile);
        when(deepSeekService.buildMessages(anyString(), isNull(), anyString()))
                .thenReturn(new JSONArray());
        when(deepSeekService.callDeepSeek(any())).thenReturn("{\"score\":90,\"reason\":\"ok\"}");

        jobAnalyzeService.analyze(1L, 1L);

        // Verify prompt includes profile and job data
        verify(deepSeekService).buildMessages(anyString(), isNull(), argThat(prompt ->
            prompt.contains("重庆大学") &&
            prompt.contains("计算机科学") &&
            prompt.contains("Java开发工程师") &&
            prompt.contains("Java、Spring Boot、MySQL") &&
            prompt.contains("负责后端开发")
        ));
    }

    private InternProfile createSampleProfile() {
        InternProfile profile = new InternProfile();
        profile.setName("张三");
        profile.setSchool("重庆大学");
        profile.setMajor("计算机科学");
        profile.setEducation("本科");
        profile.setGraduationYear("2025");
        profile.setSkills("[\"Java\",\"Spring Boot\",\"MySQL\"]");

        InternProject project = new InternProject();
        project.setProjectName("在线商城系统");
        project.setRole("后端开发");
        project.setDescription("基于Spring Boot开发的电商平台");
        profile.setProjects(List.of(project));

        return profile;
    }

    private JobPosting createSampleJob() {
        JobPosting job = new JobPosting();
        job.setId(1L);
        job.setTitle("Java开发工程师");
        job.setCompany("测试科技有限公司");
        job.setSalaryMin(new BigDecimal("10000"));
        job.setSalaryMax(new BigDecimal("20000"));
        job.setLocation("重庆");
        job.setEducation("本科");
        job.setExperience("1-3年");
        return job;
    }
}
