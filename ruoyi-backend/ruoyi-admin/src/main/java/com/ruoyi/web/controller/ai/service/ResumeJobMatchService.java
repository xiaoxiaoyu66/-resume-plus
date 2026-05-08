package com.ruoyi.web.controller.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.web.controller.ai.domain.JobPosting;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeJobMatchService {

    private static final Logger log = LoggerFactory.getLogger(ResumeJobMatchService.class);

    @Resource
    private JobVectorService jobVectorService;

    @Resource
    private JobPostingService jobPostingService;

    public List<Map<String, Object>> matchJobs(JSONObject resumeContent) {
        List<Map<String, Object>> result = new ArrayList<>();

        String profileText = buildProfileText(resumeContent);
        if (profileText.isEmpty()) {
            log.warn("resume content is empty, cannot match");
            return result;
        }

        Map<Long, Float> scores = jobVectorService.computeScores(profileText);
        if (scores.isEmpty()) {
            log.warn("no job vectors, please import jobs first");
            return result;
        }

        List<Long> topJobIds = scores.entrySet().stream()
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topJobIds.isEmpty()) return result;

        Map<Long, JobPosting> jobMap = new LinkedHashMap<>();
        for (Long id : topJobIds) {
            JobPosting job = jobPostingService.selectJobById(id);
            if (job != null) {
                jobMap.put(id, job);
            }
        }

        for (Map.Entry<Long, Float> entry : scores.entrySet()) {
            Long jobId = entry.getKey();
            JobPosting job = jobMap.get(jobId);
            if (job == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("job", jobToMap(job));
            item.put("score", entry.getValue());
            result.add(item);

            if (result.size() >= 10) break;
        }

        log.info("job match completed: {} results", result.size());
        return result;
    }

    /**
     * 岗位匹配（含简历诊断缺失项标注）
     *
     * @param resumeContent   简历内容
     * @param diagnosisMissing 诊断缺失项列表（可为 null）
     * @return 匹配结果，每项附加 diagnosisMissing 字段
     */
    public List<Map<String, Object>> matchJobs(JSONObject resumeContent, List<String> diagnosisMissing) {
        List<Map<String, Object>> result = matchJobs(resumeContent);
        if (diagnosisMissing != null && !diagnosisMissing.isEmpty()) {
            for (Map<String, Object> item : result) {
                item.put("diagnosisMissing", new ArrayList<>(diagnosisMissing));
            }
        }
        return result;
    }

    private String buildProfileText(JSONObject resume) {
        StringBuilder sb = new StringBuilder();

        JSONObject intention = resume.getJSONObject("intention");
        if (intention != null) {
            String pos = intention.getString("position");
            if (pos != null && !pos.isEmpty()) {
                sb.append("position: ").append(pos).append("\n");
            }
        }

        JSONArray skills = resume.getJSONArray("skills");
        if (skills != null && !skills.isEmpty()) {
            sb.append("skills: ");
            for (int i = 0; i < skills.size(); i++) {
                Object s = skills.get(i);
                if (s instanceof JSONObject) {
                    sb.append(((JSONObject) s).getString("name"));
                } else {
                    sb.append(s.toString());
                }
                if (i < skills.size() - 1) sb.append(", ");
            }
            sb.append("\n");
        }

        JSONArray experience = resume.getJSONArray("experience");
        if (experience != null && !experience.isEmpty()) {
            sb.append("experience:\n");
            for (int i = 0; i < experience.size(); i++) {
                JSONObject exp = experience.getJSONObject(i);
                String company = exp.getString("company");
                String position = exp.getString("position");
                String desc = exp.getString("desc");
                if (company != null) sb.append(company);
                if (position != null) sb.append(" (").append(position).append(")");
                sb.append("\n");
                if (desc != null && !desc.isEmpty()) sb.append(desc).append("\n");
            }
        }

        JSONArray projects = resume.getJSONArray("projects");
        if (projects != null && !projects.isEmpty()) {
            sb.append("projects:\n");
            for (int i = 0; i < projects.size(); i++) {
                JSONObject proj = projects.getJSONObject(i);
                String name = proj.getString("name");
                String role = proj.getString("role");
                String desc = proj.getString("desc");
                if (name != null) sb.append(name);
                if (role != null) sb.append(" (").append(role).append(")");
                sb.append("\n");
                if (desc != null && !desc.isEmpty()) sb.append(desc).append("\n");
            }
        }

        String evaluation = resume.getString("evaluation");
        if (evaluation != null && !evaluation.isEmpty()) {
            sb.append("evaluation: ").append(evaluation).append("\n");
        }

        return sb.toString();
    }

    private Map<String, Object> jobToMap(JobPosting job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", job.getId());
        m.put("title", job.getTitle());
        m.put("company", job.getCompany());
        m.put("salaryMin", job.getSalaryMin());
        m.put("salaryMax", job.getSalaryMax());
        m.put("location", job.getLocation());
        m.put("education", job.getEducation());
        m.put("experience", job.getExperience());

        String tags = job.getTags();
        if (tags != null && !tags.isEmpty()) {
            try {
                m.put("tags", JSON.parseArray(tags, String.class));
            } catch (Exception e) {
                m.put("tags", Collections.singletonList(tags));
            }
        } else {
            m.put("tags", Collections.emptyList());
        }

        m.put("description", job.getDescription());
        m.put("requirements", job.getRequirements());
        return m;
    }
}
