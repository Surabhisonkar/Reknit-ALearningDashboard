package com.learningdashboard.backend.generation.model;

/** For concepts best shown as a single labeled picture (a species, an object, a landmark). */
public record ImagePayload(
        int version,
        String imagePrompt,
        String artifactId,
        String altText
) implements VisualizationPayload {

    @Override
    public String type() { return "image"; }
}
