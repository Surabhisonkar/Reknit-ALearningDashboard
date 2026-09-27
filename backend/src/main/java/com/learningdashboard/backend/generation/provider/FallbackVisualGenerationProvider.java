package com.learningdashboard.backend.generation.provider;

import java.util.List;

public class FallbackVisualGenerationProvider implements VisualGenerationProvider {

    private final List<VisualGenerationProvider> orderedProviders;
    private final ProviderFallbackExecutor fallbackExecutor;

    public FallbackVisualGenerationProvider(List<VisualGenerationProvider> orderedProviders, ProviderFallbackExecutor fallbackExecutor) {
        this.orderedProviders = orderedProviders;
        this.fallbackExecutor = fallbackExecutor;
    }

    @Override
    public VisualAsset generateVisual(String prompt) {
        return fallbackExecutor.executeWithFallback(orderedProviders, p -> p.generateVisual(prompt), "visual generation");
    }

    @Override
    public String name() {
        return "fallback(" + orderedProviders.stream().map(VisualGenerationProvider::name).toList() + ")";
    }
}
