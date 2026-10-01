package com.learningdashboard.backend.concept;

import com.learningdashboard.backend.config.SparkMixProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Picks a page of Spark concepts in the configured mix. Each pick first
 * chooses a type bucket by weight, then a random concept inside it, so the
 * shares hold however many concepts of each type the user has. A bucket that
 * runs out drops out, and its share passes to the rest in proportion. No
 * concept appears twice in a page.
 */
@Component
public class SparkFeedSampler {

    private final SparkMixProperties mix;
    private final RandomGenerator random;

    @Autowired
    public SparkFeedSampler(SparkMixProperties mix) {
        this(mix, RandomGenerator.getDefault());
    }

    /** For tests: a seeded generator makes the picks repeatable. */
    SparkFeedSampler(SparkMixProperties mix, RandomGenerator random) {
        this.mix = mix;
        this.random = random;
    }

    public List<Concept> sample(List<Concept> pool, int limit) {
        Map<String, List<Concept>> buckets = new LinkedHashMap<>();
        for (Concept concept : pool) {
            buckets.computeIfAbsent(mix.bucketFor(concept.getVisualizationType()), key -> new ArrayList<>()).add(concept);
        }
        List<Concept> page = new ArrayList<>(Math.min(limit, pool.size()));
        while (page.size() < limit && !buckets.isEmpty()) {
            String bucket = pickBucket(buckets);
            List<Concept> members = buckets.get(bucket);
            page.add(members.remove(random.nextInt(members.size())));
            if (members.isEmpty()) {
                buckets.remove(bucket);
            }
        }
        return page;
    }

    private String pickBucket(Map<String, List<Concept>> buckets) {
        int total = 0;
        for (String bucket : buckets.keySet()) {
            total += Math.max(mix.weightFor(bucket), 0);
        }
        if (total == 0) {
            // Every remaining bucket is weighted 0: fall back to an even pick rather than stall.
            List<String> names = new ArrayList<>(buckets.keySet());
            return names.get(random.nextInt(names.size()));
        }
        int roll = random.nextInt(total);
        for (String bucket : buckets.keySet()) {
            roll -= Math.max(mix.weightFor(bucket), 0);
            if (roll < 0) {
                return bucket;
            }
        }
        throw new IllegalStateException("unreachable: the roll is always below the total weight");
    }
}
