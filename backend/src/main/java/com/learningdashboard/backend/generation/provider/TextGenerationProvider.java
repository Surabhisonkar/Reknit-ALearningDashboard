package com.learningdashboard.backend.generation.provider;

import java.util.function.Function;

/**
 * Contract for any LLM backing text output — used for both the EXPLAIN
 * pipeline (topic -> explanation) and the structuring half of the
 * VISUALIZE pipeline (concept text -> structured visualization JSON).
 * Those are two different prompts/calls (see generation.prompt), never
 * one call doing both.
 */
public interface TextGenerationProvider {
    String generateText(String systemPrompt, String userPrompt);

    /**
     * Generates text and turns it into a usable result in one step. A
     * {@code parser} that throws means "this answer is unusable" (invalid
     * JSON, wrong structure, failed content safety). A single provider can
     * only propagate that; the fallback-chain composite overrides this to
     * move on to the next provider instead, so one provider's bad answer
     * isn't the end of the request.
     */
    default <T> T generateAndParse(String systemPrompt, String userPrompt, Function<String, T> parser) {
        return parser.apply(generateText(systemPrompt, userPrompt));
    }

    /** Stable identifier used in logs, metrics tags, and circuit breaker/retry instance names. */
    String name();
}
