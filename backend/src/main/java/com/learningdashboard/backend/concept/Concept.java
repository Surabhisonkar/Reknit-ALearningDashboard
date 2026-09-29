package com.learningdashboard.backend.concept;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Server-side persistence for a generated concept — this table is what
 * replaces the old browser {@code localStorage} entirely. {@code
 * visualizationPayload} is always the normalized, validated, versioned
 * JSON produced by {@link
 * com.learningdashboard.backend.generation.validation.VisualPayloadValidator}
 * (see {@link com.learningdashboard.backend.generation.model.VisualizationPayload}),
 * never raw model output — the raw response lives only on the {@code
 * generation_jobs} row that produced this concept, for audit purposes.
 */
@Entity
@Table(name = "concepts")
public class Concept {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    /** The folder this concept is filed in, or null when unfiled. Folder-level data lives in the folder module. */
    @Column(name = "folder_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID folderId;

    @Column(name = "visualization_type", nullable = false, length = 32)
    private String visualizationType;

    @Column(name = "visualization_payload", columnDefinition = "json", nullable = false)
    private String visualizationPayload;

    @Column(name = "visualization_version", nullable = false)
    private int visualizationVersion = 1;

    /**
     * The version number of {@code concept_versions} this row's
     * title/summary/visualizationType/visualizationPayload are currently
     * caching. Every generation (including the first) writes a {@code
     * concept_versions} row; this column plus the cached fields above are
     * what keeps a plain {@code GET /api/concepts/{id}} join-free for the
     * common case, while {@code concept_versions} stays the source of
     * truth for history. See {@code ConceptVersionWriter}.
     */
    @Column(name = "current_version", nullable = false)
    private int currentVersion = 1;

    @Column(name = "source_explain_job_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID sourceExplainJobId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Concept() { }

    public Concept(UUID userId, String title, String summary, UUID folderId, String visualizationType,
                   String visualizationPayload, int visualizationVersion, UUID sourceExplainJobId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.summary = summary;
        this.folderId = folderId;
        this.visualizationType = visualizationType;
        this.visualizationPayload = visualizationPayload;
        this.visualizationVersion = visualizationVersion;
        this.sourceExplainJobId = sourceExplainJobId;
    }

    /** Used when the user resolves a duplicate-title collision by renaming rather than replacing the old one. */
    public void renameTo(String newTitle) {
        this.title = newTitle;
    }

    /** Files this concept in another folder, or unfiles it with null. Ownership of the folder is checked by the caller. */
    public void moveToFolder(UUID newFolderId) {
        this.folderId = newFolderId;
    }

    /**
     * Called once a regenerate's new {@code concept_versions} row is
     * finalized: replaces this row's denormalized cache (title/summary/
     * visualizationType/visualizationPayload/visualizationVersion) with
     * the new version's content and advances {@code currentVersion} to
     * match. Deliberately never touches {@code folderId} - the folder is
     * concept-level metadata, not part of any one version's content, so a
     * regenerate never changes it.
     */
    public void applyNewVersion(int newCurrentVersion, String title, String summary,
                                 String visualizationType, String visualizationPayload, int visualizationVersion) {
        this.currentVersion = newCurrentVersion;
        this.title = title;
        this.summary = summary;
        this.visualizationType = visualizationType;
        this.visualizationPayload = visualizationPayload;
        this.visualizationVersion = visualizationVersion;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public UUID getFolderId() { return folderId; }
    public String getVisualizationType() { return visualizationType; }
    public String getVisualizationPayload() { return visualizationPayload; }
    public int getVisualizationVersion() { return visualizationVersion; }
    public int getCurrentVersion() { return currentVersion; }
    public UUID getSourceExplainJobId() { return sourceExplainJobId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
