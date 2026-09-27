package com.learningdashboard.backend.generation.provider.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * One shared decision point for "should this provider call be retried /
 * tripped into an open circuit", instead of duplicating backoff logic in
 * every adapter. Each provider gets its own named circuit breaker and
 * retry instance (registries are keyed by name), so Claude having a bad
 * day doesn't affect Gemini's breaker state.
 *
 * <p>Only retries on likely-transient failures (timeouts, network errors,
 * 5xx); 4xx-class errors (bad input, auth) fail fast — retrying those
 * just burns quota for a guaranteed-identical failure.
 */
@Component
public class ResilientProviderExecutor {

    private static final Logger log = LoggerFactory.getLogger(ResilientProviderExecutor.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ResilientProviderExecutor(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    public <T> T execute(String providerInstanceName, Supplier<T> call) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerInstanceName, defaultCircuitBreakerConfig());
        Retry retry = retryRegistry.retry(providerInstanceName, defaultRetryConfig());

        Supplier<T> decorated = Decorators.ofSupplier(call)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .decorate();

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker OPEN for provider '{}', failing fast.", providerInstanceName);
            throw new ProviderUnavailableException(providerInstanceName, e);
        }
    }

    private CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordException(this::isRetryableFailure)
                .build();
    }

    private RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // 1 initial + 2 retries, matches AI_MAX_RETRIES default
                .waitDuration(Duration.ofMillis(400))
                .retryExceptions(RuntimeException.class)
                .retryOnException(this::isRetryableFailure)
                .build();
    }

    private boolean isRetryableFailure(Throwable error) {
        if (error instanceof HttpStatusCodeException httpError) {
            int status = httpError.getStatusCode().value();
            // 429 is retryable (rate limited, backoff and try again); other
            // 4xx are not (retrying a malformed request never succeeds).
            return status == 429 || status >= 500;
        }
        return true; // network errors, timeouts, etc.
    }
}
