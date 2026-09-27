package com.learningdashboard.backend.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Metadata row for a generated S3 object (an image today; animation
 * frames/narration audio later). {@code expiresAt} drives both an S3
 * bucket lifecycle rule (see infra/) and a periodic cleanup job that
 * purges rows/objects for jobs that never got attached to a saved
 * concept (an orphaned generation attempt).
 */
@Entity
@Table(name = "artifacts")
public class Artifact {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(name = "generation_job_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID generationJobId;

    @Column(name = "concept_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptId;

    /** Which exact concept_versions row this artifact belongs to - set alongside conceptId in attachToConcept, so there's never ambiguity about which version an artifact was generated for, even for a concept's first-ever version. */
    @Column(name = "concept_version_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptVersionId;

    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(nullable = false)
    private int version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Artifact() { }

    public Artifact(UUID userId, UUID generationJobId, UUID conceptId, String s3Key, String contentType, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.generationJobId = generationJobId;
        this.conceptId = conceptId;
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public void attachToConcept(UUID conceptId, UUID conceptVersionId) {
        this.conceptId = conceptId;
        this.conceptVersionId = conceptVersionId;
        this.expiresAt = null; // no longer orphaned, don't expire it
    }

    public void markOrphanExpiry(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getGenerationJobId() { return generationJobId; }
    public UUID getConceptId() { return conceptId; }
    public UUID getConceptVersionId() { return conceptVersionId; }
    public String getS3Key() { return s3Key; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
