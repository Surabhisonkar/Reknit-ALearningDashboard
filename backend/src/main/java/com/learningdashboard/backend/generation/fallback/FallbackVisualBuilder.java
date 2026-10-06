package com.learningdashboard.backend.generation.fallback;

import com.learningdashboard.backend.generation.model.VisualizationPayload;

/**
 * Strategy for building one visualization type from a {@link
 * ConceptOutline} with no AI call. One bean per type, registered by
 * {@link #type()} in {@link FallbackVisualizationService} - a mind map, a
 * flowchart and an animation each need their own layout logic, so each
 * lives in its own class (adding a type is one new bean, never a switch).
 *
 * <p>Implementations must be pure (no I/O) and must stay inside the limits
 * {@code VisualPayloadValidator} enforces for their type.
 */
public interface FallbackVisualBuilder {

    /** The visualization type this builder produces: mind_map | diagram | animation. */
    String type();

    VisualizationPayload build(ConceptOutline outline);
}
