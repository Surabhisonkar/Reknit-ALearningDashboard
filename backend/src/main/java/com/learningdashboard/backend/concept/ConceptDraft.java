package com.learningdashboard.backend.concept;

import java.util.List;
import java.util.UUID;

/**
 * Everything needed to turn one generation into a {@link ConceptVersion}
 * - the concept module's own view of a "draft", deliberately free of any
 * generation-module type ({@code GenerationJob}, {@code
 * VisualizationPayload}). The payload travels as already-validated,
 * already-serialized JSON; this module stores it, it never interprets it.
 *
 * @param generationJobId        the job that produced this draft (for traceability)
 * @param sourceExplainJobId     optional EXPLAIN job the text came from
 * @param suggestedFolder        the model's folder suggestion, may be blank
 * @param visualizationPayloadJson normalized payload JSON, exactly as it will be stored
 * @param payloadSchemaVersion   the payload's own schema version ({@code visualization_version})
 * @param artifactIds            generated assets referenced by the payload, still orphaned
 */
public record ConceptDraft(
        UUID generationJobId,
        UUID sourceExplainJobId,
        String title,
        String summary,
        String suggestedFolder,
        String visualizationType,
        String visualizationPayloadJson,
        int payloadSchemaVersion,
        List<UUID> artifactIds
) {
    public ConceptDraft {
        artifactIds = artifactIds == null ? List.of() : List.copyOf(artifactIds);
        suggestedFolder = suggestedFolder == null ? "" : suggestedFolder;
    }

    /** Same draft with the title replaced - used when the user resolves a duplicate by renaming. */
    public ConceptDraft withTitle(String newTitle) {
        return new ConceptDraft(generationJobId, sourceExplainJobId, newTitle, summary, suggestedFolder,
                visualizationType, visualizationPayloadJson, payloadSchemaVersion, artifactIds);
    }
}
