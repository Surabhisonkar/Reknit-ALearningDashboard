package com.learningdashboard.backend.generation.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.anthropic.ClaudeTextProvider;
import com.learningdashboard.backend.generation.provider.gemini.GeminiEmbeddingProvider;
import com.learningdashboard.backend.generation.provider.gemini.GeminiTextProvider;
import com.learningdashboard.backend.generation.provider.gemini.GeminiVisualProvider;
import com.learningdashboard.backend.generation.provider.openai.OpenAiTextProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * The only class in the codebase that knows which concrete provider
 * classes exist. Everything else (pipelines, controllers, tests) depends
 * only on {@link TextGenerationProvider}, {@link VisualGenerationProvider},
 * {@link EmbeddingProvider} — the beans exposed here are the
 * fallback-chain composites, built from the {@code *_PROVIDER_ORDER} env
 * vars, so a provider outage transparently falls through to the next
 * configured one.
 *
 * <p>To add a provider: write the adapter class, add one {@code @Bean}
 * method here, add its name to the relevant {@code *_PROVIDER_ORDER}
 * config. No other file changes. Concrete provider beans are
 * {@code @Lazy} + individually try/catch-wrapped so a missing API key
 * for a provider nobody's using yet doesn't stop the app from starting.
 */
@Configuration
public class ProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(ProviderFactory.class);

    // ---- concrete provider beans -----------------------------------------

    @Bean
    @Lazy
    public ClaudeTextProvider claudeTextProvider(AiProperties props, ResilientProviderExecutor executor, ObjectMapper mapper) {
        return new ClaudeTextProvider(props, executor, mapper);
    }

    @Bean
    @Lazy
    public GeminiTextProvider geminiTextProvider(AiProperties props, ResilientProviderExecutor executor, ObjectMapper mapper) {
        return new GeminiTextProvider(props, executor, mapper);
    }

    @Bean
    @Lazy
    public OpenAiTextProvider openAiTextProvider(AiProperties props, ResilientProviderExecutor executor, ObjectMapper mapper) {
        return new OpenAiTextProvider(props, executor, mapper);
    }

    @Bean
    @Lazy
    public GeminiVisualProvider geminiVisualProvider(AiProperties props, ResilientProviderExecutor executor, ObjectMapper mapper) {
        return new GeminiVisualProvider(props, executor, mapper);
    }

    @Bean
    @Lazy
    public GeminiEmbeddingProvider geminiEmbeddingProvider(AiProperties props, ResilientProviderExecutor executor, ObjectMapper mapper) {
        return new GeminiEmbeddingProvider(props, executor, mapper);
    }

    // ---- fallback-chain composites (these are what pipelines inject) ------

    @Bean
    @Primary
    public TextGenerationProvider textGenerationProvider(
            AiProperties props, ProviderFallbackExecutor fallbackExecutor,
            @Lazy ClaudeTextProvider claude, @Lazy GeminiTextProvider geminiText, @Lazy OpenAiTextProvider openAi) {

        Map<String, TextGenerationProvider> byName = new LinkedHashMap<>();
        byName.put("claude", claude);
        byName.put("gemini", geminiText);
        byName.put("openai", openAi);

        return new FallbackTextGenerationProvider(resolveOrder("text generation", props.getTextProviderOrder(), byName), fallbackExecutor);
    }

    @Bean
    @Primary
    public VisualGenerationProvider visualGenerationProvider(
            AiProperties props, ProviderFallbackExecutor fallbackExecutor, @Lazy GeminiVisualProvider geminiVisual) {

        Map<String, VisualGenerationProvider> byName = Map.of("gemini", geminiVisual);
        return new FallbackVisualGenerationProvider(resolveOrder("visual generation", props.getVisualProviderOrder(), byName), fallbackExecutor);
    }

    @Bean
    @Primary
    public EmbeddingProvider embeddingProvider(
            AiProperties props, ProviderFallbackExecutor fallbackExecutor, @Lazy GeminiEmbeddingProvider geminiEmbedding) {

        Map<String, EmbeddingProvider> byName = Map.of("gemini", geminiEmbedding);
        return new FallbackEmbeddingProvider(resolveOrder("embedding", props.getEmbeddingProviderOrder(), byName), fallbackExecutor);
    }

    /**
     * Resolves the configured, ordered provider-name list against the
     * available beans. A bean whose construction fails (e.g. missing API
     * key, since the concrete beans above are {@code @Lazy}) is logged
     * and skipped rather than crashing the whole chain — the remaining
     * configured providers still work.
     *
     * <p>Also logs, at startup, whether the resolved chain actually has
     * redundancy. This exists because of a real incident: {@code
     * TEXT_PROVIDER_ORDER} defaulted to {@code claude,gemini}, but with
     * only {@code GEMINI_API_KEY} set, {@code claude} silently dropped out
     * above and Gemini alone was left to fail a request outright — a
     * single point of failure that was otherwise invisible until it broke
     * in front of a user. Now it's a line in the startup log instead.
     */
    private <T> List<T> resolveOrder(String capabilityName, List<String> configuredOrder, Map<String, T> byName) {
        List<T> resolved = configuredOrder.stream()
                .map(String::trim)
                .filter(nameKey -> {
                    if (!byName.containsKey(nameKey)) {
                        log.warn("Unknown provider '{}' in configured order, skipping.", nameKey);
                        return false;
                    }
                    return isConstructible(byName.get(nameKey), nameKey);
                })
                .map(byName::get)
                .toList();

        if (resolved.isEmpty()) {
            log.error("No usable {} providers are configured - every request will fail with ALL_PROVIDERS_FAILED "
                    + "until at least one provider's API key is set.", capabilityName);
        } else if (resolved.size() == 1) {
            log.warn("Only one {} provider is usable ({} of {} configured could be constructed) - there is no "
                    + "real fallback redundancy right now. Set another provider's API key "
                    + "(ANTHROPIC_API_KEY / GEMINI_API_KEY / OPENAI_API_KEY as applicable) to restore it.",
                    capabilityName, resolved.size(), configuredOrder.size());
        }

        return resolved;
    }

    private boolean isConstructible(Object lazyProxy, String nameKey) {
        try {
            // Force the @Lazy proxy to initialize now so a missing-API-key
            // failure surfaces here (skip this provider) rather than the
            // first time a request actually needs it.
            lazyProxy.toString();
            return true;
        } catch (Exception e) {
            log.error("Provider '{}' is not usable ({}), removing it from the fallback chain.", nameKey, e.getMessage());
            return false;
        }
    }
}
