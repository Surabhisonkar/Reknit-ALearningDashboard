package com.learningdashboard.backend.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ConceptResponse(
        UUID id,
        String title,
        String summary,
        String folder,
        String visualizationType,
        JsonNode visualization,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
