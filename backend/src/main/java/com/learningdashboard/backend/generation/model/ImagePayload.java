package com.learningdashboard.backend.generation.model;

import java.util.List;

/**
 * For concepts best shown as a single labeled picture (a species, an
 * object, a landmark) - "Illustration" in the UI. The image model is told
 * to draw no text at all; {@code labels} (version 2) are the callouts the
 * frontend draws on top as SVG, so the words on screen are always exact.
 */
public record ImagePayload(
        int version,
        String imagePrompt,
        String artifactId,
        String altText,
        List<Label> labels
) implements VisualizationPayload {

    @Override
    public String type() { return "image"; }

    /** {@code x}/{@code y} are the anchor point as a fraction (0-1) of the image's width and height. */
    public record Label(String id, String text, String explanation, Double x, Double y) { }
}
