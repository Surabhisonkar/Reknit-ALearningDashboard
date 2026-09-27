package com.learningdashboard.backend.concept;

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
 * One immutable-once-finalized row per generation of a concept — including
 * the very first one. {@code version} is 1-based and contiguous per
 * concept (never reused, never deleted), so {@link Concept#getCurrentVersion()}
 * doubles as "how many versions exist" without a COUNT query.
 *
 * <p>Uses the same two-phase save pattern as {@link Concept} itself:
 * created with a placeholder {@code visualizationPayload} so it has an id
 * for generated visual assets ({@code artifacts.concept_version_id}) to
 * attach to, then {@link #finalizeContent} fills in the real payload once
 * those assets exist. (Current code - {@code ConceptVersionWriter} - generates
 * assets first, so it constructs the row with its final payload directly.)
 */
@Entity
@Table(name = "concept_versions")
public class ConceptVersion {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "concept_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "visualization_type", nullable = false, length = 32)
    private String visualizationType;

    @Column(name = "visualization_payload", columnDefinition = "json", nullable = false)
    private String visualizationPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConceptVersion() { }

    public ConceptVersion(UUID conceptId, int version, String title, String summary,
                           String visualizationType, String visualizationPayload) {
        this.id = UUID.randomUUID();
        this.conceptId = conceptId;
        this.version = version;
        this.title = title;
        this.summary = summary;
        this.visualizationType = visualizationType;
        this.visualizationPayload = visualizationPayload;
    }

    /** Fills in the real payload once generated visual assets exist and have been attached to this version. */
    public void finalizeContent(String visualizationPayload) {
        this.visualizationPayload = visualizationPayload;
    }

    public UUID getId() { return id; }
    public UUID getConceptId() { return conceptId; }
    public int getVersion() { return version; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getVisualizationType() { return visualizationType; }
    public String getVisualizationPayload() { return visualizationPayload; }
    public Instant getCreatedAt() { return createdAt; }
}
