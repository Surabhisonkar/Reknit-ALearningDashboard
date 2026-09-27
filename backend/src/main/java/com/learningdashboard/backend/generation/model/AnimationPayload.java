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
 */
public record AnimationPayload(
        int version,
        List<Scene> scenes
) implements VisualizationPayload {

    @Override
    public String type() { return "animation"; }

    /**
     * {@code assetArtifactIds} start empty when the LLM proposes the
     * scene; {@code VisualAssetGenerator} fills them in after calling
     * {@code VisualGenerationProvider} for scenes the model flagged as
     * needing a generated image ({@code needsVisualAsset}).
     */
    public record Scene(
            String id, int order, String title, String narration,
            double durationSeconds, String transitionToNext,
            boolean needsVisualAsset, String visualPrompt,
            List<String> assetArtifactIds
    ) { }
}
