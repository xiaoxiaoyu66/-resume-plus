package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.domain.JobPosting;
import com.ruoyi.web.controller.ai.mapper.JobPostingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JobPostingService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingService.class);

    @Resource
    private JobPostingMapper jobPostingMapper;

    @Resource
    private JobVectorService jobVectorService;

    public List<JobPosting> selectJobList(JobPosting job) {
        return jobPostingMapper.selectJobList(job);
    }

    public JobPosting selectJobById(Long id) {
        return jobPostingMapper.selectJobById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int insertJob(JobPosting job) {
        job.setDeleted(0);
        return jobPostingMapper.insertJob(job);
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateJob(JobPosting job) {
        return jobPostingMapper.updateJob(job);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteJobById(Long id) {
        return jobPostingMapper.deleteJobById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importJobs(List<JobPosting> jobs) {
        int total = jobs.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < jobs.size(); i++) {
            try {
                JobPosting job = jobs.get(i);
                if (job.getTitle() == null || job.getTitle().isEmpty()) {
                    failed++;
                    errors.add("第" + (i + 1) + "行: 职位名称不能为空");
                    continue;
                }
                job.setDeleted(0);
                jobPostingMapper.insertJob(job);
                // 自动嵌入向量（Layer 3）
                try {
                    jobVectorService.embedSingleJob(job);
                } catch (Exception e) {
                    // 嵌入失败不影响导入
                    log.warn("岗位向量化失败: jobId={}, {}", job.getId(), e.getMessage());
                }
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("第" + (i + 1) + "行: " + e.getMessage());
            }
        }

        return Map.of(
            "total", total,
            "success", success,
            "failed", failed,
            "errors", errors
        );
    }
}
