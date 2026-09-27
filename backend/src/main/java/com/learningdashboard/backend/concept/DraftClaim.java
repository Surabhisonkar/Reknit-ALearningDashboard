package com.learningdashboard.backend.concept;

import java.util.Optional;
import java.util.UUID;

/**
 * Result of {@link ConceptDraftSource#claim}: the draft itself, plus the
 * id of the concept it was already saved as, if any. A non-empty
 * {@code savedConceptId} makes confirm-save idempotent (a double click or
 * client retry returns the existing concept instead of creating a second).
 */
public record DraftClaim(ConceptDraft draft, Optional<UUID> savedConceptId) {
    public DraftClaim {
        savedConceptId = savedConceptId == null ? Optional.empty() : savedConceptId;
    }
}
