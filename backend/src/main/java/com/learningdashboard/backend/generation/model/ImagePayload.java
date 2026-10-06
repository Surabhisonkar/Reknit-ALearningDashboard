package com.learningdashboard.backend.generation.model;

/**
 * For concepts best shown as a single labeled picture (a species, an object, a landmark).
 *
 * <p>{@code fallbackDiagram} is the same idea as a flowchart, written by
 * the text model in the same call that wrote {@code imagePrompt}. It is
 * what the user gets if the image model fails (see {@code
 * VisualAssetGenerator}), and what the frontend's "Show as diagram"
 * toggle renders when the picture came out wrong. Nullable: concepts
 * stored before this field existed, and answers where the model left it
 * out, simply have none.
 */
public record ImagePayload(
        int version,
        String imagePrompt,
        String artifactId,
        String altText,
        DiagramPayload fallbackDiagram
) implements VisualizationPayload {

    @Override
    public String type() { return "image"; }
}
