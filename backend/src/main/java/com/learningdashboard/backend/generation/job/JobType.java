package com.learningdashboard.backend.generation.job;

/**
 * EXPLAIN and VISUALIZE are deliberately separate job types with separate
 * pipelines and separate provider calls — never one prompt asking an LLM
 * to both explain a topic and structure a visualization at once. This is
 * what lets the "Explain" button exist as its own user-facing step
 * (topic -> explanation, user reviews/edits, then -> visualization) and
 * lets each step use whichever provider is actually best at that task.
 */
public enum JobType {
    EXPLAIN,
    VISUALIZE,
    /**
     * (Re-)embeds one saved concept for RAG. Enqueued after a concept is
     * saved or regenerated, so the embedding-provider call happens in the
     * worker - the api service never calls an AI provider itself.
     */
    INDEX_CONCEPT,
    /** One question in a Spark "Ask the AI" chat about a saved concept (Phase 8). Nothing is saved on the concept. */
    ASK_CONCEPT,
    /** Condenses a Spark chat into a short note and saves it on the concept (Phase 8). */
    CHAT_TO_NOTE
}
