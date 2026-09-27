package com.learningdashboard.backend.generation.provider;

import java.util.List;

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

    @Override
    public String name() {
        return "fallback(" + orderedProviders.stream().map(TextGenerationProvider::name).toList() + ")";
    }
}
