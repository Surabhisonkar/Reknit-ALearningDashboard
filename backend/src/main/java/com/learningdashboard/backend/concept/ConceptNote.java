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

/** A short note saved on a concept - today, an AI-condensed Spark chat. Immutable once written. */
@Entity
@Table(name = "concept_notes")
public class ConceptNote {

    public static final String SOURCE_CHAT = "chat";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "concept_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptId;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 16)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConceptNote() { }

    public ConceptNote(UUID conceptId, UUID userId, String content, String source) {
        this.id = UUID.randomUUID();
        this.conceptId = conceptId;
        this.userId = userId;
        this.content = content;
        this.source = source;
    }

    public UUID getId() { return id; }
    public UUID getConceptId() { return conceptId; }
    public UUID getUserId() { return userId; }
    public String getContent() { return content; }
    public String getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
}
