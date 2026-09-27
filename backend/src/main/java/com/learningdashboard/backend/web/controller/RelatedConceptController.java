package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.config.RelatedConceptsProperties;
import com.learningdashboard.backend.rag.RetrievalService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.RelatedConceptResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG surfaced as a feature: "concepts you've saved that relate to this
 * one", across folders. A thin read - ownership check, then a
 * stored-vector similarity lookup (no AI provider call; see {@link
 * RetrievalService#findRelatedToConcept}). Its own controller rather than
 * another method on {@code ConceptController} (single responsibility);
 * Spring routes this literal sub-path alongside that controller's
 * {@code /{id}} routes without conflict.
 */
@RestController
@RequestMapping("/api/concepts")
public class RelatedConceptController {

    static final int MAX_LIMIT = 20;

    private final ConceptService conceptService;
    private final RetrievalService retrievalService;
    private final RelatedConceptsProperties properties;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    public RelatedConceptController(ConceptService conceptService, RetrievalService retrievalService,
                                    RelatedConceptsProperties properties, CurrentUserService currentUserService,
                                    ResponseMapper responseMapper) {
        this.conceptService = conceptService;
        this.retrievalService = retrievalService;
        this.properties = properties;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
    }

    /**
     * Nearest first; {@code []} when nothing is close enough (or the
     * concept hasn't been indexed yet). {@code limit} and {@code
     * maxDistance} are optional overrides used by the calibration script -
     * clamped, and they only ever read the caller's own concepts.
     */
    @GetMapping("/{id}/related")
    public List<RelatedConceptResponse> related(@PathVariable UUID id,
                                                @RequestParam(required = false) Integer limit,
                                                @RequestParam(required = false) Double maxDistance) {
        var user = currentUserService.requireCurrentUser();
        conceptService.requireOwnedConcept(id, user.getId()); // 404 for missing and for not-yours alike

        int topK = clamp(limit == null ? properties.getTopK() : limit, 1, MAX_LIMIT);
        double cutoff = clamp(maxDistance == null ? properties.getMaxDistance() : maxDistance, 0.0, 2.0);

        return retrievalService.findRelatedToConcept(id, user.getId(), topK, cutoff).stream()
                .map(responseMapper::toRelatedConceptResponse)
                .toList();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Double.isNaN(value) ? max : Math.max(min, Math.min(max, value));
    }
}
