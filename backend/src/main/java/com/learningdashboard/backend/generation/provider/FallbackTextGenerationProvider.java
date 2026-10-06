package com.learningdashboard.backend.generation.provider;

import java.util.List;
import java.util.function.Function;

/** Tries each configured text provider in order until one succeeds. */
public class FallbackTextGenerationProvider implements TextGenerationProvider {

    private final List<TextGenerationProvider> orderedProviders;
    private final ProviderFallbackExecutor fallbackExecutor;

    public FallbackTextGenerationProvider(List<TextGenerationProvider> orderedProviders, ProviderFallbackExecutor fallbackExecutor) {
        this.orderedProviders = orderedProviders;
        this.fallbackExecutor = fallbackExecutor;
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return fallbackExecutor.executeWithFallback(
                orderedProviders, p -> p.generateText(systemPrompt, userPrompt), "text generation");
    }

    /** Parsing happens inside the chain, so an unusable answer from one provider falls through to the next. */
    @Override
    public <T> T generateAndParse(String systemPrompt, String userPrompt, Function<String, T> parser) {
        return fallbackExecutor.executeWithFallback(
                orderedProviders, p -> parser.apply(p.generateText(systemPrompt, userPrompt)), "text generation");
    }

    @Override
    public String name() {
        return "fallback(" + orderedProviders.stream().map(TextGenerationProvider::name).toList() + ")";
    }
}
