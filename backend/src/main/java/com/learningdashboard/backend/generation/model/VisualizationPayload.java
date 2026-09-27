package com.learningdashboard.backend.generation.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The stable domain contract between backend and frontend: the React app
 * maps <em>this</em> shape into renderer components, never raw LLM JSON.
 * The pipeline is: LLM raw JSON -&gt; {@link
 * com.learningdashboard.backend.generation.validation.VisualPayloadValidator}
 * enforces this schema -&gt; a typed instance of one of these records ->
 * persisted as normalized JSON -&gt; served to the frontend keyed strictly
 * off {@code type}.
 *
 * <p>Four types, matching the product's visual formats: {@code mind_map}
 * (branching/relational ideas), {@code diagram} (structured processes/
 * architectures), {@code animation} (sequential/temporal ideas), {@code
 * image} (a single concrete visual subject that doesn't need structure).
 * Every type carries {@code version} so a schema change later doesn't
 * break rendering of already-stored concepts — the frontend mapper can
 * branch on version per type if the shape ever evolves.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MindMapPayload.class, name = "mind_map"),
        @JsonSubTypes.Type(value = DiagramPayload.class, name = "diagram"),
        @JsonSubTypes.Type(value = AnimationPayload.class, name = "animation"),
        @JsonSubTypes.Type(value = ImagePayload.class, name = "image"),
})
public sealed interface VisualizationPayload permits MindMapPayload, DiagramPayload, AnimationPayload, ImagePayload {
    String type();
    int version();
}
