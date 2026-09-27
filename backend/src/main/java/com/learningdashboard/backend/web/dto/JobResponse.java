package com.learningdashboard.backend.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

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
