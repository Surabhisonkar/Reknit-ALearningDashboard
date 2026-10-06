package com.learningdashboard.backend.generation.fallback;

import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The last line of the visual failsafe: builds a visualization from the
 * user's own text with no AI provider involved. Used by {@code
 * VisualizePipeline} when no provider could be reached, or when every
 * provider's answer was unusable (invalid JSON, wrong structure, or
 * rejected by content safety).
 *
 * <p>Builders are discovered from the Spring context and registered by
 * type, so supporting another type needs no change here (Open/Closed).
 */
@Component
public class FallbackVisualizationService {

    static final String NEUTRAL_TITLE = "Your notes";
    static final String NEUTRAL_SUMMARY = "A visual built from your notes.";

    /** Requested type -> the coded type that stands in for it. "auto" is absent on purpose (see resolveType). */
    private static final Map<String, String> STAND_IN_BY_REQUESTED_TYPE = Map.of(
            "mind_map", "mind_map",
            "diagram", "diagram",
            "animation", "animation",
            "image", "diagram");

    private final ConceptTextOutliner outliner;
    private final ContentSafetyValidator contentSafetyValidator;
    private final Map<String, FallbackVisualBuilder> buildersByType;

    public FallbackVisualizationService(ConceptTextOutliner outliner, ContentSafetyValidator contentSafetyValidator,
                                        List<FallbackVisualBuilder> builders) {
        this.outliner = outliner;
        this.contentSafetyValidator = contentSafetyValidator;
        Map<String, FallbackVisualBuilder> byType = new HashMap<>();
        for (FallbackVisualBuilder builder : builders) {
            FallbackVisualBuilder previous = byType.put(builder.type(), builder);
            if (previous != null) {
                throw new IllegalStateException("Two FallbackVisualBuilders registered for " + builder.type() + ": "
                        + previous.getClass().getSimpleName() + " and " + builder.getClass().getSimpleName());
            }
        }
        this.buildersByType = Map.copyOf(byType);
    }

    /** A complete draft (title, summary, payload) when the AI produced nothing usable at all. */
    public VisualizationDraft buildDraft(String preferredType, String conceptText) {
        ConceptOutline outline = outliner.outline(conceptText);
        VisualizationPayload payload = builderFor(resolveType(preferredType, outline)).build(outline);
        return new VisualizationDraft(
                safeOrElse("title", outline.title(), NEUTRAL_TITLE),
                safeOrElse("summary", outline.summary(), NEUTRAL_SUMMARY),
                "", payload, List.of());
    }

    /** Just the payload - for when the AI's title and summary are fine but its visual could not be produced. */
    public VisualizationPayload buildPayload(String type, String conceptText) {
        return builderFor(type).build(outliner.outline(conceptText));
    }

    /**
     * Which coded visual stands in for the requested one. An image has no
     * coded equivalent, so it becomes a flowchart. With "auto" nobody chose
     * a type, so the text decides: step-like text animates, the rest maps.
     */
    static String resolveType(String preferredType, ConceptOutline outline) {
        String standIn = preferredType == null ? null : STAND_IN_BY_REQUESTED_TYPE.get(preferredType);
        if (standIn != null) {
            return standIn;
        }
        return outline.sequential() ? "animation" : "mind_map";
    }

    private FallbackVisualBuilder builderFor(String type) {
        FallbackVisualBuilder builder = buildersByType.get(type);
        if (builder == null) {
            throw new IllegalStateException("No FallbackVisualBuilder registered for type " + type);
        }
        return builder;
    }

    /** The user's own words can still trip the operator's disallowed-terms list; never fail the failsafe over it. */
    private String safeOrElse(String field, String value, String neutral) {
        if (value == null || value.isBlank()) {
            return neutral;
        }
        try {
            contentSafetyValidator.validate(field, value);
            return value;
        } catch (GenerationException rejected) {
            return neutral;
        }
    }
}
