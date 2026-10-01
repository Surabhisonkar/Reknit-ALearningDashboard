package com.learningdashboard.backend.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Spark feed's concept mix (user decision, Phase 8), shared by type
 * "bucket". Games (the other 20% of cards) are added by the frontend; these
 * are the concept shares: animation 60, mind map 15, and images + diagrams
 * together 5. Only the ratios matter.
 */
@ConfigurationProperties(prefix = "app.spark.mix")
public class SparkMixProperties {

    /** Bucket name -> weight. */
    private Map<String, Integer> weights = new LinkedHashMap<>(Map.of("animation", 60, "mind_map", 15, "still", 5));

    /** Visualization type -> bucket, for types that share one. Unlisted types are their own bucket. */
    private Map<String, String> bucketOf = new LinkedHashMap<>(Map.of("image", "still", "diagram", "still"));

    /** Weight for a bucket missing from {@link #weights} (e.g. a future visualization type). */
    private int defaultWeight = 5;

    public Map<String, Integer> getWeights() { return weights; }
    public void setWeights(Map<String, Integer> weights) { this.weights = weights; }
    public Map<String, String> getBucketOf() { return bucketOf; }
    public void setBucketOf(Map<String, String> bucketOf) { this.bucketOf = bucketOf; }
    public int getDefaultWeight() { return defaultWeight; }
    public void setDefaultWeight(int defaultWeight) { this.defaultWeight = defaultWeight; }

    public String bucketFor(String visualizationType) {
        return bucketOf.getOrDefault(visualizationType, visualizationType);
    }

    public int weightFor(String bucket) {
        return weights.getOrDefault(bucket, defaultWeight);
    }
}
