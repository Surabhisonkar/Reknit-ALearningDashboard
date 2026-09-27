package com.learningdashboard.backend.generation.provider;

import java.util.List;

public class FallbackEmbeddingProvider implements EmbeddingProvider {

    private final List<EmbeddingProvider> orderedProviders;
    private final ProviderFallbackExecutor fallbackExecutor;

    public FallbackEmbeddingProvider(List<EmbeddingProvider> orderedProviders, ProviderFallbackExecutor fallbackExecutor) {
        this.orderedProviders = orderedProviders;
        this.fallbackExecutor = fallbackExecutor;
    }

    @Override
    public float[] embed(String text) {
        return fallbackExecutor.executeWithFallback(orderedProviders, p -> p.embed(text), "embedding");
    }

    @Override
    public String name() {
        return "fallback(" + orderedProviders.stream().map(EmbeddingProvider::name).toList() + ")";
    }
}
