package com.learningdashboard.backend.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code folder} is the folder's name, or "" when unfiled (unchanged contract).
 * {@code folderId} and {@code folderColor} (a palette key) are omitted when
 * unfiled, via the app-wide non_null Jackson setting.
 */
public record ConceptResponse(
        UUID id,
        String title,
        String summary,
        String folder,
        UUID folderId,
        String folderColor,
        String visualizationType,
        JsonNode visualization,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
