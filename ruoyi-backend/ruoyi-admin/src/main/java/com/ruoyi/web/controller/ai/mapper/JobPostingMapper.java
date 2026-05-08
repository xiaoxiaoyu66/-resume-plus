package com.ruoyi.web.controller.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.ruoyi.web.controller.ai.domain.JobPosting;
import java.util.List;

@Mapper
public interface JobPostingMapper {

    List<JobPosting> selectJobList(JobPosting job);

    JobPosting selectJobById(Long id);

    int insertJob(JobPosting job);

    int updateJob(JobPosting job);

    int deleteJobById(Long id);

    int insertJobBatch(List<JobPosting> jobs);
}
