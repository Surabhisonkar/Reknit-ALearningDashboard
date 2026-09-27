package com.learningdashboard.backend.generation.model;

import java.util.List;
import java.util.UUID;

/**
 * What one Visualize generation produces: validated, safety-checked
 * content plus the ids of any generated assets (already uploaded, still
 * orphaned). Pure data - it knows nothing about whether it will become a
 * brand-new concept or a new version of an existing one; that decision
 * belongs to {@code VisualizeJobHandler}.
 */
public record VisualizationDraft(
        String title,
        String summary,
        String suggestedFolder,
        VisualizationPayload payload,
        List<UUID> artifactIds
) {
    public VisualizationDraft {
        artifactIds = artifactIds == null ? List.of() : List.copyOf(artifactIds);
        suggestedFolder = suggestedFolder == null ? "" : suggestedFolder;
    }
}
