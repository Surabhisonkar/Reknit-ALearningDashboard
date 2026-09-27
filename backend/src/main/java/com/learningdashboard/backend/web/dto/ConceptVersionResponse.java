package com.learningdashboard.backend.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Full content of one historical version - GET /api/concepts/{id}/versions/{version}. The concept's own cached fields (from ConceptResponse) don't carry historical content, so the switcher fetches this to render a non-current version. */
public record ConceptVersionResponse(
        int version,
        String title,
        String summary,
        String visualizationType,
        JsonNode visualization,
        Instant createdAt
) {
}
