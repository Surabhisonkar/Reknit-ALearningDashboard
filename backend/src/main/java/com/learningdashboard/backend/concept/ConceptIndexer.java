package com.learningdashboard.backend.concept;

import java.util.UUID;

/**
 * Port: "make sure this concept's RAG embedding reflects its current
 * title/summary". Implementations decide how and when (the default one
 * enqueues an async job after the current transaction commits, so the
 * api process never calls an embedding provider itself).
 */
public interface ConceptIndexer {
    void requestIndexing(UUID conceptId, UUID userId);
}
