package com.learningdashboard.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code conceptText} is whichever explanation the user chose to run
 * with — their own notes, or the AI's from the Explain step — the
 * frontend decides which one to send, not this endpoint.
 */
public class VisualizeRequest {

    /** Required for a new capture; optional for a regenerate (see {@link #isConceptTextPresentUnlessRegenerate}). */
    @Size(max = 6000, message = "conceptText must be at most 6000 characters")
    private String conceptText;

    @Pattern(regexp = "mind_map|diagram|animation|image|auto",
            message = "preferredVisualizationType must be one of mind_map, diagram, animation, image, auto")
    /**
     * Null when omitted: a new capture treats that as "auto" (see
     * GenerationJobService.submitVisualizeJob); a regenerate treats it as
     * "same as the original generation".
     */
    private String preferredVisualizationType;

    /** Optional - links this visualization back to the EXPLAIN job that produced the text, for audit/traceability. */
    private UUID sourceExplainJobId;

    /**
     * Optional - when set, this is a regenerate: the result is appended as
     * a new version of this existing concept instead of creating a new
     * one. Must belong to the requesting user; ownership is checked
     * synchronously in VisualizeController before the job is even
     * enqueued, so a cross-tenant id 404s immediately rather than failing
     * the job later in the worker.
     */
    private UUID conceptId;

    /**
     * {@code conceptText} may only be omitted on a regenerate, where the
     * server reuses the concept's original input. Violations come back as
     * the usual 400 "Invalid request." with this message in {@code details}.
     */
    @JsonIgnore
    @AssertTrue(message = "conceptText must not be blank")
    public boolean isConceptTextPresentUnlessRegenerate() {
        return conceptId != null || (conceptText != null && !conceptText.isBlank());
    }

    public String getConceptText() { return conceptText; }
    public void setConceptText(String conceptText) { this.conceptText = conceptText; }
    public String getPreferredVisualizationType() { return preferredVisualizationType; }
    public void setPreferredVisualizationType(String v) { this.preferredVisualizationType = v; }
    public UUID getSourceExplainJobId() { return sourceExplainJobId; }
    public void setSourceExplainJobId(UUID sourceExplainJobId) { this.sourceExplainJobId = sourceExplainJobId; }
    public UUID getConceptId() { return conceptId; }
    public void setConceptId(UUID conceptId) { this.conceptId = conceptId; }
}
