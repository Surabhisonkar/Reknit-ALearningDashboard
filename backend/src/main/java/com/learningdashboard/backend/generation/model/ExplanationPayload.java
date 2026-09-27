package com.learningdashboard.backend.generation.model;

import java.util.List;

/**
 * The AI-generated explanation, structured so the frontend can render it
 * with real formatting (headings, bullets, distinct styling per section
 * type) instead of a single plain-text paragraph. Mirrors the same
 * "backend produces structure, frontend renders it, never raw LLM text
 * on screen" principle as {@link VisualizationPayload}.
 *
 * <p>{@code sections} always include at least one {@code analogy} and one
 * {@code example} (enforced by the prompt and validated by {@code
 * ExplanationValidator}) — grounding an explanation in something
 * familiar and something concrete is the actual point of this feature,
 * not an optional extra.
 */
public record ExplanationPayload(
        int version,
        String suggestedTitle,
        String overview,
        List<Section> sections
) {
    /**
     * A section's content is either short prose ({@code body}) or a
     * short bullet list ({@code bullets}) - never both at length, so the
     * renderer never has to guess which one to show.
     */
    public record Section(String heading, String type, String body, List<String> bullets) { }
}
