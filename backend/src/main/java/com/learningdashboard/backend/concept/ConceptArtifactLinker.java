package com.learningdashboard.backend.concept;

import java.util.Collection;
import java.util.UUID;

/**
 * Port: permanently attach generated assets to the exact concept version
 * they were generated for (clearing their orphan expiry). Implemented by
 * the storage module - the concept module never touches artifact rows or
 * S3 directly.
 */
public interface ConceptArtifactLinker {

    /**
     * Attaches every artifact in {@code artifactIds} that belongs to {@code userId}.
     * Ids that don't exist or belong to someone else are rejected (not silently skipped).
     */
    void link(Collection<UUID> artifactIds, UUID userId, UUID conceptId, UUID conceptVersionId);
}
