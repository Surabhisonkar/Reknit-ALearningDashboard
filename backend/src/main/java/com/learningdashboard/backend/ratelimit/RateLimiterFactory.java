package com.learningdashboard.backend.ratelimit;

import com.learningdashboard.backend.config.RateLimitProperties;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RateLimiterFactory {

    @Bean(name = "explainRateLimiter")
    public DistributedRateLimiter explainRateLimiter(@Lazy ProxyManager<byte[]> proxyManager, RateLimitProperties props) {
        var cfg = props.getExplain();
        return new DistributedRateLimiter(proxyManager, "rl:explain", Duration.ofSeconds(cfg.getWindowSeconds()), cfg.getMaxRequests());
    }

    @Bean(name = "visualizeRateLimiter")
    public DistributedRateLimiter visualizeRateLimiter(@Lazy ProxyManager<byte[]> proxyManager, RateLimitProperties props) {
        var cfg = props.getVisualize();
        return new DistributedRateLimiter(proxyManager, "rl:visualize", Duration.ofSeconds(cfg.getWindowSeconds()), cfg.getMaxRequests());
    }

    @Bean(name = "askRateLimiter")
    public DistributedRateLimiter askRateLimiter(@Lazy ProxyManager<byte[]> proxyManager, RateLimitProperties props) {
        var cfg = props.getAsk();
        return new DistributedRateLimiter(proxyManager, "rl:ask", Duration.ofSeconds(cfg.getWindowSeconds()), cfg.getMaxRequests());
    }

    /**
     * Rate limiting is keyed by authenticated user id, not IP — every
     * request past SecurityConfig already carries a validated Cognito
     * identity, and per-user is the meaningful cost/abuse boundary for
     * paid AI calls (shared IPs — offices, campuses, carrier-grade NAT —
     * would otherwise throttle unrelated users together).
     */
    public static String userKey(UUID userId) {
        return userId.toString();
    }
}
