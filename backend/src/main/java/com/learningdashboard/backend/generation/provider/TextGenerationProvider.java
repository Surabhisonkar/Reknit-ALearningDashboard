package com.learningdashboard.backend.generation.provider;

/**
 * Contract for any LLM backing text output — used for both the EXPLAIN
 * pipeline (topic -> explanation) and the structuring half of the
 * VISUALIZE pipeline (concept text -> structured visualization JSON).
 * Those are two different prompts/calls (see generation.prompt), never
 * one call doing both.
 */
public interface TextGenerationProvider {
    String generateText(String systemPrompt, String userPrompt);

    /** Stable identifier used in logs, metrics tags, and circuit breaker/retry instance names. */
    String name();
}
