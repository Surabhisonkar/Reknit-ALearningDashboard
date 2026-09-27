package com.learningdashboard.backend.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row per concept, storing its embedding vector for server-side RAG
 * retrieval. MySQL has no native vector type (unlike Postgres+pgvector),
 * so the vector is stored as a JSON array of floats via {@link
 * VectorJsonConverter} and similarity search runs in the application
 * layer — see {@link RetrievalService} / {@link CosineSimilarity}. This
 * is a deliberate, documented scaling trade-off (see the migration's
 * comment on this table); it's fine at the per-user concept counts this
 * product expects.
 *
 * <p>Deliberately 1:1 with {@code concepts} (unique constraint on
 * concept_id) rather than chunked — concept summaries are already
 * short, so chunking/multiple-vectors-per-concept isn't needed at this
 * scale; if concepts grow to multi-paragraph documents later, this
 * table (and {@code RetrievalService}) is the only place that needs to
 * change to support chunking.
 */
@Entity
@Table(name = "concept_embeddings")
public class ConceptEmbedding {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "concept_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptId;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Convert(converter = VectorJsonConverter.class)
    @Column(nullable = false, columnDefinition = "json")
    private float[] embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConceptEmbedding() { }

    public ConceptEmbedding(UUID conceptId, UUID userId, float[] embedding) {
        this.id = UUID.randomUUID();
        this.conceptId = conceptId;
        this.userId = userId;
        this.embedding = embedding;
    }

    /** Re-embeds this row in place after the concept it belongs to was edited (e.g. a regenerate produced a new title/summary) - see EmbeddingIndexService. */
    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public UUID getId() { return id; }
    public UUID getConceptId() { return conceptId; }
    public UUID getUserId() { return userId; }
    public float[] getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
}
