package com.learningdashboard.backend.rag;

/**
 * Standard cosine distance between two equal-length vectors, as
 * {@code 1 - cosineSimilarity} - lower means more similar, matching the
 * "distance" convention {@link RelatedConcept#distance} already used
 * (chosen back when this ran as a pgvector {@code <=>} query, which
 * returns distance the same way).
 */
final class CosineSimilarity {

    private CosineSimilarity() { }

    static double distance(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Cannot compare embeddings of different dimensions (%d vs %d) - did the embedding model change?"
                            .formatted(a.length, b.length));
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 1.0; // a zero vector has no meaningful direction - treat as maximally dissimilar
        }

        double cosineSimilarity = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1.0 - cosineSimilarity;
    }
}
