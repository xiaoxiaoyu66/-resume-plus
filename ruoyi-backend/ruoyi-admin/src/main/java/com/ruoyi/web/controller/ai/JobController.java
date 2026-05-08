package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.controller.ai.domain.JobPosting;
import com.ruoyi.web.controller.ai.service.JobAnalyzeService;
import com.ruoyi.web.controller.ai.service.JobPostingService;
import com.ruoyi.web.controller.ai.service.JobVectorService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/jobs")
public class JobController extends BaseController {

    @Resource
    private JobPostingService jobPostingService;

    @Resource
    private JobAnalyzeService jobAnalyzeService;

    @Resource
    private JobVectorService jobVectorService;

    @GetMapping("/list")
    public TableDataInfo list(JobPosting job) {
        startPage();
        List<JobPosting> list = jobPostingService.selectJobList(job);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        return AjaxResult.success(jobPostingService.selectJobById(id));
    }

    @PostMapping
    public AjaxResult add(@RequestBody JobPosting job) {
        jobPostingService.insertJob(job);
        return AjaxResult.success("新增成功");
    }

    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody JobPosting job) {
        job.setId(id);
        jobPostingService.updateJob(job);
        return AjaxResult.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        jobPostingService.deleteJobById(id);
        return AjaxResult.success("删除成功");
    }

    /**
     * AI 匹配度深度分析（Layer 2）
     */
    @PostMapping("/{id}/analyze")
    public AjaxResult analyze(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> result = jobAnalyzeService.analyze(id, userId);
        return AjaxResult.success(result);
    }

    /**
     * 向量相似度匹配（Layer 3）
     * 传入用户画像文本，返回 jobId→相似度 映射
     */
    @PostMapping("/vector-scores")
    public AjaxResult vectorScores(@RequestBody Map<String, String> body) {
        String profileText = body.get("profileText");
        if (profileText == null || profileText.trim().isEmpty()) {
            return AjaxResult.error("画像文本不能为空");
        }
        Map<Long, Float> scores = jobVectorService.computeScores(profileText);
        return AjaxResult.success(scores);
    }

    /**
     * 批量嵌入所有岗位向量
     */
    @PostMapping("/embed-all")
    public AjaxResult embedAll() {
        int count = jobVectorService.embedAllJobs();
        return AjaxResult.success("批量向量化完成，共 " + count + " 条");
    }

    @PostMapping("/import")
    public AjaxResult importJobs(@RequestBody List<JobPosting> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return AjaxResult.error("导入数据不能为空");
        }
        Map<String, Object> result = jobPostingService.importJobs(jobs);
        return AjaxResult.success(result);
    }
}
