package com.learningdashboard.backend.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * One Redis connection, one Bucket4j {@link ProxyManager}, shared by
 * every named rate limiter bean in {@link RateLimiterFactory}. This is
 * the fix for the single biggest gap in the earlier migration: with an
 * in-memory limiter, N ECS tasks behind the ALB each enforce the limit
 * independently, so a client could get up to N times their intended
 * quota. Backing the bucket state in Redis (ElastiCache in production)
 * makes the limit genuinely shared across every task.
 */
@Configuration
public class RedisProxyManagerConfig {

    private RedisClient redisClient;
    @Lazy
    @Bean
    public ProxyManager<byte[]> bucketProxyManager(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {

        redisClient = RedisClient.create(RedisURI.Builder.redis(host, port).build());
        var connection = redisClient.connect(ByteArrayCodec.INSTANCE);

        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }

    @PreDestroy
    void shutdown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
