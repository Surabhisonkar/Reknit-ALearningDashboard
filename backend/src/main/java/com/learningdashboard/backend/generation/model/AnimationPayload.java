package com.learningdashboard.backend.generation.model;

import java.util.List;

/**
 * Real animation data, not just an ordered text list: each scene carries
 * timing and a transition, and can reference generated visual assets.
 * The <em>number</em> of scenes, their duration, and which scenes need a
 * generated image are all decided by the LLM per topic in {@code
 * VisualizationPromptBuilder} — a photosynthesis animation and a
 * French-Revolution-timeline animation legitimately need different scene
 * counts and pacing, so nothing here hardcodes a fixed scene template.
 *
 * <p>Version 2 is a storyboard: each scene also lists its {@code
 * visualElements}, and {@code videoArtifactId} points at the MP4 the
 * Remotion render service made from it (empty when no renderer is
 * configured - the frontend then plays the same composition live).
 */
public record AnimationPayload(
        int version,
        List<Scene> scenes,
        String videoArtifactId
) implements VisualizationPayload {

    @Override
    public String type() { return "animation"; }

    /**
     * {@code assetArtifactIds} start empty when the LLM proposes the
     * scene; {@code VisualAssetGenerator} fills them in after calling
     * {@code VisualGenerationProvider} for scenes the model flagged as
     * needing a generated image ({@code needsVisualAsset}).
     *
     * <p>{@code layout} (optional, version 2) names how the composition
     * arranges the scene's elements - steps, cycle, compare, before_after or
     * focus - so the model needn't choose positions; null or "free" keeps
     * each element's own x/y.
     */
    public record Scene(
            String id, int order, String title, String narration,
            double durationSeconds, String transitionToNext,
            boolean needsVisualAsset, String visualPrompt,
            List<String> assetArtifactIds,
            List<VisualElement> visualElements,
            String layout
    ) {
        public Scene withAssetArtifactIds(List<String> ids) {
            return new Scene(id, order, title, narration, durationSeconds, transitionToNext,
                    needsVisualAsset, visualPrompt, ids, visualElements, layout);
        }
    }

    /**
     * One thing on screen in a scene (version 2 storyboards), drawn by the
     * Remotion composition. {@code x}/{@code y} are percentages of the
     * frame, unused when the scene has a layout; {@code enterAtSeconds} is
     * relative to the scene's start. Arrows use {@code fromId}/{@code toId}
     * instead of a position, and their {@code text} is a short label.
     * {@code icon} is an optional emoji drawn above a box's or circle's text.
     */
    public record VisualElement(
            String id, String kind, String text, Double x, Double y,
            Double enterAtSeconds, String animation, String color,
            String fromId, String toId, String icon
    ) { }
}
