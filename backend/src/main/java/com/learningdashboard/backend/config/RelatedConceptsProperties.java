package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for the user-facing "Related concepts" panel
 * ({@code GET /api/concepts/{id}/related}). Both values are env-driven so
 * they can be calibrated against real embeddings without a redeploy of
 * code - see {@code docs/design/phase-f-related-concepts.md} section 6.
 */
@ConfigurationProperties(prefix = "app.rag.related")
public class RelatedConceptsProperties {

    /** Maximum related concepts returned by default. */
    private int topK = 5;

    /**
     * Maximum cosine distance (1 - cosine similarity) for a concept to
     * count as related. Lower = stricter. Without a cut-off, "related"
     * would just mean "nearest", even between unrelated topics.
     */
    private double maxDistance = 0.35;

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double maxDistance) { this.maxDistance = maxDistance; }
}
