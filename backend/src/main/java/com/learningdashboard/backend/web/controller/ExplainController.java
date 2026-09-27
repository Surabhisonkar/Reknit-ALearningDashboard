package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.generation.job.GenerationJob;
import com.learningdashboard.backend.generation.job.GenerationJobService;
import com.learningdashboard.backend.ratelimit.DistributedRateLimiter;
import com.learningdashboard.backend.ratelimit.RateLimiterFactory;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.ExplainRequest;
import com.learningdashboard.backend.web.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the "Explain" button. Creates an EXPLAIN job and returns
 * immediately with {@code 202 Accepted} - the actual LLM call happens in
 * the worker service; poll {@code GET /api/jobs/{id}} for the result.
 */
@RestController
@RequestMapping("/api/jobs/explain")
public class ExplainController {

    private final GenerationJobService jobService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;
    private final DistributedRateLimiter rateLimiter;

    public ExplainController(GenerationJobService jobService, CurrentUserService currentUserService,
                              ResponseMapper responseMapper, @Qualifier("explainRateLimiter") DistributedRateLimiter rateLimiter) {
        this.jobService = jobService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<?> explain(@Valid @RequestBody ExplainRequest request) {
        var user = currentUserService.requireCurrentUser();

        if (!rateLimiter.tryConsume(RateLimiterFactory.userKey(user.getId()))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("error", "Too many requests. Please wait a moment and try again."));
        }

        GenerationJob job = jobService.submitExplainJob(user.getId(), request.getTopic(), request.getUserNotes());
        return ResponseEntity.accepted().body(responseMapper.toJobResponse(job));
    }
}
