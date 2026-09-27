package com.learningdashboard.backend.web.dto;

import java.time.Instant;

public record ArtifactDownloadResponse(String downloadUrl, Instant expiresAt) {
}
