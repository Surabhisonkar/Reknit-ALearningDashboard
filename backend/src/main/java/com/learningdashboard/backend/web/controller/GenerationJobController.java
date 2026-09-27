package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.generation.job.GenerationJobService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.JobResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The frontend polls this after getting a 202 from either job-creation
 * endpoint. Ownership is enforced by {@code requireOwnedJob} - a job id
 * that exists but belongs to someone else 404s, same as one that
 * doesn't exist at all.
 */
@RestController
@RequestMapping("/api/jobs")
public class GenerationJobController {

    private final GenerationJobService jobService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    public GenerationJobController(GenerationJobService jobService, CurrentUserService currentUserService, ResponseMapper responseMapper) {
        this.jobService = jobService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        var user = currentUserService.requireCurrentUser();
        var job = jobService.requireOwnedJob(id, user.getId());
        return responseMapper.toJobResponse(job);
    }
}
