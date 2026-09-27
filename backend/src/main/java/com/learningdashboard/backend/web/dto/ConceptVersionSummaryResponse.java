package com.learningdashboard.backend.web.dto;

import java.time.Instant;

/** One entry in the version switcher's list - GET /api/concepts/{id}/versions. No payload here; see ConceptVersionResponse for the full content of one version. */
public record ConceptVersionSummaryResponse(
        int version,
        String title,
        Instant createdAt
) {
}
