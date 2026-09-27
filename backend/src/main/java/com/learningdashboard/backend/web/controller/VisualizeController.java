package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.generation.job.GenerationJob;
import com.learningdashboard.backend.generation.job.GenerationJobService;
import com.learningdashboard.backend.ratelimit.DistributedRateLimiter;
import com.learningdashboard.backend.ratelimit.RateLimiterFactory;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.JobResponse;
import com.learningdashboard.backend.web.dto.VisualizeRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs "Visualize". Creates a VISUALIZE job and returns {@code 202 Accepted}.
 * Without {@code conceptId} the finished job holds a <em>draft</em> (saved
 * only via {@code POST /api/concepts}); with it, the job appends a new
 * version to that already-saved concept.
 */
@RestController
@RequestMapping("/api/jobs/visualize")
public class VisualizeController {

    private final GenerationJobService jobService;
    private final ConceptService conceptService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;
    private final DistributedRateLimiter rateLimiter;

    public VisualizeController(GenerationJobService jobService, ConceptService conceptService, CurrentUserService currentUserService,
                                ResponseMapper responseMapper, @Qualifier("visualizeRateLimiter") DistributedRateLimiter rateLimiter) {
        this.jobService = jobService;
        this.conceptService = conceptService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<?> visualize(@Valid @RequestBody VisualizeRequest request) {
        var user = currentUserService.requireCurrentUser();

        if (!rateLimiter.tryConsume(RateLimiterFactory.userKey(user.getId()))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("error", "Too many requests. Please wait a moment and try again."));
        }

        // A regenerate (conceptId set): ownership is checked below, synchronously,
        // so a cross-tenant or nonexistent id 404s immediately instead of the
        // job silently failing later in the worker.
        GenerationJob job;
        if (request.getConceptId() != null) {
            // Post-save regenerate: appends a new version once the worker finishes.
            // Anything the client leaves out is taken from the concept's original generation.
            Concept concept = conceptService.requireOwnedConcept(request.getConceptId(), user.getId());
            job = jobService.submitRegenerateJob(user.getId(), concept.getId(), request.getConceptText(),
                    request.getPreferredVisualizationType(), concept.getTitle() + "\n\n" + concept.getSummary());
        } else {
            // New capture: the job's result is a reviewable draft; nothing is saved until POST /api/concepts.
            job = jobService.submitVisualizeJob(user.getId(), request.getConceptText(),
                    request.getPreferredVisualizationType(), request.getSourceExplainJobId(), null);
        }
        return ResponseEntity.accepted().body(responseMapper.toJobResponse(job));
    }
}
