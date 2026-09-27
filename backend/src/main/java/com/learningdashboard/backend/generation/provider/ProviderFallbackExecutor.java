package com.learningdashboard.backend.generation.provider;

import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Shared "try providers in configured order, fall through on failure"
 * logic. One small generic class instead of three near-identical retry
 * loops copy-pasted into each Fallback*Provider — the three decorators
 * that follow are then ~15 lines each, just wiring this to their
 * specific interface's method signature (kept separate rather than one
 * mega-generic class, since {@code generateText}, {@code generateVisual},
 * and {@code embed} have different signatures — collapsing them further
 * would need reflection or a lowest-common-denominator signature, which
 * would cost more clarity than the few lines it'd save).
 */
@Component
public class ProviderFallbackExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProviderFallbackExecutor.class);

    public <P, R> R executeWithFallback(List<P> providers, Function<P, R> call, String capabilityName) {
        if (providers.isEmpty()) {
            throw new IllegalStateException("No providers configured for " + capabilityName + ".");
        }

        RuntimeException lastError = null;
        for (P provider : providers) {
            try {
                return call.apply(provider);
            } catch (RuntimeException error) {
                lastError = error;
                log.warn("{} provider failed, trying next in fallback chain: {}", capabilityName, error.getMessage());
            }
        }

        throw new GenerationException(
                "All configured " + capabilityName + " providers failed.",
                GenerationException.Code.ALL_PROVIDERS_FAILED,
                lastError);
    }
}
