package com.learningdashboard.backend.rag;

import java.util.UUID;

/** Server-side RAG retrieval result - never computed or stored in the browser. */
public record RelatedConcept(UUID conceptId, String title, String summary, double distance) {
}
