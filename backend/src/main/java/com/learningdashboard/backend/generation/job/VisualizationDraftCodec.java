package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.concept.ConceptDraft;
import com.learningdashboard.backend.concept.VersionAppended;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The single owner of the Visualize job's {@code result_payload} JSON
 * shapes. Written by {@link VisualizeJobHandler}, read back by {@link
 * JobConceptDraftSource}, polled by the frontend - so this is the one
 * file to change if the draft shape ever evolves.
 *
 * <pre>
 * DRAFT   { "kind":"DRAFT", "title", "summary", "suggestedFolder", "visualizationType",
 *           "visualization": {...payload...}, "artifactIds": [..] }
 * VERSION { "kind":"VERSION", "version": n, "duplicateTitleConceptId"?: uuid }
 * </pre>
 */
@Component
public class VisualizationDraftCodec {

    public static final String KIND = "kind";
    public static final String KIND_DRAFT = "DRAFT";
    public static final String KIND_VERSION = "VERSION";

    private final JobJson jobJson;

    public VisualizationDraftCodec(JobJson jobJson) {
        this.jobJson = jobJson;
    }

    public String toDraftResultJson(VisualizationDraft draft) {
        ObjectNode node = jobJson.mapper().createObjectNode();
        node.put(KIND, KIND_DRAFT);
        node.put("title", draft.title());
        node.put("summary", draft.summary());
        node.put("suggestedFolder", draft.suggestedFolder());
        node.put("visualizationType", draft.payload().type());
        node.set("visualization", jobJson.read(jobJson.write(draft.payload())));
        ArrayNode ids = node.putArray("artifactIds");
        draft.artifactIds().forEach(id -> ids.add(id.toString()));
        return node.toString();
    }

    public String toVersionResultJson(VersionAppended appended) {
        ObjectNode node = jobJson.mapper().createObjectNode();
        node.put(KIND, KIND_VERSION);
        if (appended.duplicateTitleConceptId() != null) {
            node.put("duplicateTitleConceptId", appended.duplicateTitleConceptId().toString());
        }
        node.put("version", appended.version());
        return node.toString();
    }

    /** In-memory conversion for the regenerate path (no JSON round trip of the draft itself). */
    public ConceptDraft toConceptDraft(VisualizationDraft draft, UUID generationJobId, UUID sourceExplainJobId) {
        return new ConceptDraft(generationJobId, sourceExplainJobId, draft.title(), draft.summary(),
                draft.suggestedFolder(), draft.payload().type(), jobJson.write(draft.payload()),
                draft.payload().version(), draft.artifactIds());
    }

    public boolean isDraft(JsonNode resultPayload) {
        return resultPayload != null && KIND_DRAFT.equals(resultPayload.path(KIND).asText(null));
    }

    /** Rebuilds the draft stored on a completed job's result_payload. Caller must have checked {@link #isDraft}. */
    public ConceptDraft readDraft(JsonNode resultPayload, UUID generationJobId, UUID sourceExplainJobId) {
        JsonNode visualization = resultPayload.path("visualization");
        String title = resultPayload.path("title").asText(null);
        String summary = resultPayload.path("summary").asText(null);
        String type = resultPayload.path("visualizationType").asText(visualization.path("type").asText(null));
        if (title == null || summary == null || type == null || !visualization.isObject()) {
            throw new GenerationException("Stored draft is incomplete.", GenerationException.Code.SCHEMA_VALIDATION_FAILED);
        }

        List<UUID> artifactIds = new ArrayList<>();
        resultPayload.path("artifactIds").forEach(id -> artifactIds.add(UUID.fromString(id.asText())));

        return new ConceptDraft(generationJobId, sourceExplainJobId, title, summary,
                resultPayload.path("suggestedFolder").asText(""), type, visualization.toString(),
                visualization.path("version").asInt(1), artifactIds);
    }
}
