package com.learningdashboard.backend.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String displayName,
        boolean deletionPending,
        Instant deletionEffectiveAt
) {
}
