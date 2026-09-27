package com.learningdashboard.backend.generation.provider;

/**
 * Contract for any model backing visual *asset* generation (image bytes
 * today; individual animation frames/narration audio later — the return
 * type is deliberately generic bytes+mimeType, not "an image", so a
 * future provider generating a different media type is still a drop-in
 * implementation).
 */
public interface VisualGenerationProvider {
    VisualAsset generateVisual(String prompt);

    String name();

    record VisualAsset(byte[] bytes, String mimeType) { }
}
