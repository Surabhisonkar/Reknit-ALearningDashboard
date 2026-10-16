package com.learningdashboard.backend.web.dto;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record JobResponse(
        UUID id,
        String jobType,
        String status,
        JsonNode resultPayload,
        UUID conceptId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
