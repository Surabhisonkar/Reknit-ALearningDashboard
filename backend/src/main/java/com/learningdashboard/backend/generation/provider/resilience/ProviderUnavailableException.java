package com.learningdashboard.backend.generation.provider.resilience;

/**
 * Thrown when a single provider's circuit breaker is open (i.e. it's
 * been failing enough that we're deliberately not calling it right now).
 * Caught by {@link com.learningdashboard.backend.generation.provider.ProviderFallbackExecutor}
 * exactly like any other provider failure — the next provider in the
 * configured fallback chain gets tried immediately, no wasted retry.
 */
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String providerName, Throwable cause) {
        super("Provider '" + providerName + "' is temporarily unavailable (circuit open).", cause);
    }
}
