package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.generation.job.GenerationJob;
import com.learningdashboard.backend.generation.job.GenerationJobService;
import com.learningdashboard.backend.ratelimit.DistributedRateLimiter;
import com.learningdashboard.backend.ratelimit.RateLimiterFactory;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.ChatNoteRequest;
import com.learningdashboard.backend.web.dto.ConceptAskRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spark's "Ask the AI" (Phase 8). Both routes enqueue a worker job and answer
 * 202 at once - the api never calls an AI provider. Ownership is checked
 * before enqueueing, so a foreign or unknown concept is a 404 now rather than
 * a job that fails later. Both routes share the "ask" rate limit.
 */
@RestController
@RequestMapping("/api/concepts/{id}")
public class ConceptChatController {

    private final GenerationJobService jobService;
    private final ConceptService conceptService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;
    private final DistributedRateLimiter rateLimiter;

    public ConceptChatController(GenerationJobService jobService, ConceptService conceptService,
                                 CurrentUserService currentUserService, ResponseMapper responseMapper,
                                 @Qualifier("askRateLimiter") DistributedRateLimiter rateLimiter) {
        this.jobService = jobService;
        this.conceptService = conceptService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@PathVariable UUID id, @Valid @RequestBody ConceptAskRequest request) {
        UUID userId = currentUserService.requireCurrentUser().getId();
        return enqueue(userId, id, () -> jobService.submitAskJob(userId, id, request.historyMessages(), request.getQuestion()));
    }

    @PostMapping("/notes/from-chat")
    public ResponseEntity<?> saveNote(@PathVariable UUID id, @Valid @RequestBody ChatNoteRequest request) {
        UUID userId = currentUserService.requireCurrentUser().getId();
        return enqueue(userId, id, () -> jobService.submitChatToNoteJob(userId, id, request.historyMessages()));
    }

    private ResponseEntity<?> enqueue(UUID userId, UUID conceptId, Supplier<GenerationJob> submit) {
        conceptService.requireOwnedConcept(conceptId, userId);
        if (!rateLimiter.tryConsume(RateLimiterFactory.userKey(userId))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please wait a moment and try again."));
        }
        return ResponseEntity.accepted().body(responseMapper.toJobResponse(submit.get()));
    }
}
