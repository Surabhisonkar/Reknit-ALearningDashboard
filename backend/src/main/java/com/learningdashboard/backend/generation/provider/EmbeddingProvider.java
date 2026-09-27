package com.learningdashboard.backend.generation.provider;

/** Contract for any model backing text embeddings, used by server-side RAG. */
public interface EmbeddingProvider {
    float[] embed(String text);

    String name();
}
