package com.learningdashboard.backend.concept;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Post-save regenerate: appends a freshly generated draft to an existing
 * concept as version {@code currentVersion + 1}. Older versions are never
 * modified or deleted - they stay reachable via {@code GET
 * /api/concepts/{id}/versions/{n}}. (Pre-save "try again" on Capture is
 * a different thing entirely: it just produces a new draft.)
 */
@Service
public class ConceptRegenerationService {

    private final ConceptService conceptService;
    private final ConceptVersionWriter versionWriter;

    public ConceptRegenerationService(ConceptService conceptService, ConceptVersionWriter versionWriter) {
        this.conceptService = conceptService;
        this.versionWriter = versionWriter;
    }

    /**
     * Must be called inside a transaction (the worker's job-processing
     * transaction). Not {@code @Transactional} itself so a failure here is
     * recorded on the job row instead of poisoning the outer transaction.
     */
    public VersionAppended appendVersion(UUID conceptId, UUID userId, ConceptDraft draft) {
        // Row lock: two regenerates of the same concept finishing at once
        // serialize here instead of both claiming the same version number.
        Concept concept = conceptService.requireOwnedConceptForUpdate(conceptId, userId);
        int newVersion = concept.getCurrentVersion() + 1;

        Concept saved = versionWriter.write(concept, newVersion, draft);

        UUID duplicateTitleConceptId = conceptService.findTitleCollision(userId, saved.getTitle(), saved.getId())
                .map(Concept::getId)
                .orElse(null);
        return new VersionAppended(saved.getId(), newVersion, duplicateTitleConceptId);
    }
}
