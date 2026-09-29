package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.folder.FolderColor;
import java.time.Instant;
import java.util.UUID;

/** {@code color} serializes as its palette key, e.g. "teal". */
public record FolderResponse(
        UUID id,
        String name,
        FolderColor color,
        long conceptCount,
        Instant createdAt
) {
}
