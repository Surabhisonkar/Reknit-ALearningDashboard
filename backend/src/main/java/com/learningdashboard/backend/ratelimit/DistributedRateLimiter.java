package com.learningdashboard.backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Same public shape as the earlier in-memory limiter
 * ({@code tryConsume(key)}), now backed by Redis via the shared {@link
 * ProxyManager} — swapping the backing store was a config-level change
 * to this constructor, not a rewrite of any call site.
 */
public class DistributedRateLimiter {

    private final ProxyManager<byte[]> proxyManager;
    private final String namePrefix;
    private final Supplier<BucketConfiguration> configSupplier;

    public DistributedRateLimiter(ProxyManager<byte[]> proxyManager, String namePrefix, Duration window, int maxRequests) {
        this.proxyManager = proxyManager;
        this.namePrefix = namePrefix;
        this.configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(maxRequests).refillIntervally(maxRequests, window).build())
                .build();
    }

    public boolean tryConsume(String clientKey) {
        byte[] redisKey = (namePrefix + ":" + clientKey).getBytes(StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.builder().build(redisKey, configSupplier);
        return bucket.tryConsume(1);
    }
}
