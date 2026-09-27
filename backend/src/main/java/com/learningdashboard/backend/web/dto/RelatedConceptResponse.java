package com.learningdashboard.backend.web.dto;

import java.util.UUID;

/**
 * One entry of {@code GET /api/concepts/{id}/related}. {@code distance} is
 * cosine distance (lower = more related) - returned for calibration and
 * debugging; the UI doesn't display it.
 */
public record RelatedConceptResponse(UUID id, String title, String summary, double distance) {
}
