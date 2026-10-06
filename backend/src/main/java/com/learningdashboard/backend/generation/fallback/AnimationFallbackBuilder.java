package com.learningdashboard.backend.generation.fallback;

import com.learningdashboard.backend.generation.model.AnimationPayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Animation: one scene per point, in order, each timed to how long its
 * narration takes to read. No scene asks for a generated image
 * ({@code needsVisualAsset=false}) - the frontend plays its own coded
 * motion for picture-less scenes, so this needs no provider at all.
 */
@Component
public class AnimationFallbackBuilder implements FallbackVisualBuilder {

    /** VisualPayloadValidator allows 12 scenes. */
    static final int MAX_SCENES = 12;
    private static final double MIN_SECONDS = 3;
    private static final double MAX_SECONDS = 12;
    /** Comfortable on-screen reading pace, slower than silent reading on purpose. */
    private static final double WORDS_PER_SECOND = 2.5;

    @Override
    public String type() {
        return "animation";
    }

    @Override
    public VisualizationPayload build(ConceptOutline outline) {
        List<AnimationPayload.Scene> scenes = new ArrayList<>();

        boolean summaryIsItsOwnPoint = outline.points().stream().anyMatch(p -> p.detail().equals(outline.summary()));
        if (!summaryIsItsOwnPoint && !outline.summary().isBlank()) {
            scenes.add(scene("intro", 0, outline.title(), outline.summary()));
        }

        for (ConceptOutline.Point point : outline.points()) {
            if (scenes.size() >= MAX_SCENES) {
                break;
            }
            scenes.add(scene("s" + (scenes.size() + 1), scenes.size(), point.label(), point.detail()));
        }

        return new AnimationPayload(1, scenes);
    }

    private AnimationPayload.Scene scene(String id, int order, String title, String narration) {
        String safeNarration = ConceptTextOutliner.shorten(narration, 600);
        return new AnimationPayload.Scene(id, order, ConceptTextOutliner.shorten(title, 80), safeNarration,
                durationFor(safeNarration), "fade", false, "", List.of());
    }

    static double durationFor(String narration) {
        int words = narration.isBlank() ? 0 : narration.trim().split("\\s+").length;
        double seconds = 2 + words / WORDS_PER_SECOND;
        double rounded = Math.round(seconds * 2) / 2.0;
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, rounded));
    }
}
