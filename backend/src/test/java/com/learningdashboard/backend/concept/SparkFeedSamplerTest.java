package com.learningdashboard.backend.concept;

import static org.assertj.core.api.Assertions.assertThat;

import com.learningdashboard.backend.config.SparkMixProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SparkFeedSamplerTest {

    private final UUID userId = UUID.randomUUID();
    private final SparkMixProperties mix = new SparkMixProperties();

    private List<Concept> pool(int animations, int mindMaps, int images, int diagrams) {
        List<Concept> pool = new ArrayList<>();
        add(pool, "animation", animations);
        add(pool, "mind_map", mindMaps);
        add(pool, "image", images);
        add(pool, "diagram", diagrams);
        return pool;
    }

    private void add(List<Concept> pool, String type, int count) {
        for (int i = 0; i < count; i++) {
            pool.add(new Concept(userId, type + i, "s", null, type, "{}", 1, null));
        }
    }

    /** First pick of many independent draws, so the mix is measured without supply running out. */
    private Map<String, Integer> firstPickShares(List<Concept> pool, int draws) {
        Map<String, Integer> counts = new HashMap<>();
        SplittableRandom seeds = new SplittableRandom(7);
        for (int i = 0; i < draws; i++) {
            SparkFeedSampler sampler = new SparkFeedSampler(mix, new SplittableRandom(seeds.nextLong()));
            String type = sampler.sample(pool, 1).get(0).getVisualizationType();
            counts.merge(mix.bucketFor(type), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void picksFollowTheBucketWeightsNotTheNumberOfConceptsPerType() {
        // Far more mind maps than animations: per-concept weighting would drown the animations.
        Map<String, Integer> shares = firstPickShares(pool(3, 60, 20, 20), 8000);

        // Expected 60 : 15 : 5 of 80 -> 75% / 18.75% / 6.25%.
        assertThat(shares.get("animation") / 8000.0).isBetween(0.72, 0.78);
        assertThat(shares.get("mind_map") / 8000.0).isBetween(0.16, 0.215);
        assertThat(shares.get("still") / 8000.0).isBetween(0.045, 0.08);
    }

    @Test
    void imagesAndDiagramsShareOneStillBucket() {
        assertThat(mix.bucketFor("image")).isEqualTo("still");
        assertThat(mix.bucketFor("diagram")).isEqualTo("still");
        assertThat(mix.bucketFor("animation")).isEqualTo("animation");
    }

    @Test
    void anEmptyBucketsShareGoesToTheOthersAndThePageStillFills() {
        List<Concept> pool = pool(0, 4, 2, 0);

        List<Concept> page = new SparkFeedSampler(mix, new SplittableRandom(1)).sample(pool, 6);

        assertThat(page).hasSize(6).containsExactlyInAnyOrderElementsOf(pool);
    }

    @Test
    void neverRepeatsAConceptAndStopsAtTheLimit() {
        List<Concept> pool = pool(10, 5, 2, 2);

        List<Concept> page = new SparkFeedSampler(mix, new SplittableRandom(3)).sample(pool, 8);

        assertThat(page).hasSize(8);
        assertThat(new HashSet<>(page)).hasSize(8);
    }

    @Test
    void anUnknownFutureTypeGetsTheDefaultWeightInsteadOfBeingDropped() {
        List<Concept> pool = new ArrayList<>();
        add(pool, "timeline", 3);

        assertThat(new SparkFeedSampler(mix, new SplittableRandom(5)).sample(pool, 3)).hasSize(3);
    }

    @Test
    void anEmptyPoolGivesAnEmptyPage() {
        assertThat(new SparkFeedSampler(mix, new SplittableRandom(9)).sample(List.of(), 5)).isEmpty();
    }
}
