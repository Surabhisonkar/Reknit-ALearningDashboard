package com.learningdashboard.backend.folder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * A user's folder - a "playlist" of concepts with a name and a palette colour.
 * Names are unique per user, compared case-insensitively but accent-sensitively
 * (the column's utf8mb4_0900_as_ci collation, see V3__folders.sql). Concepts
 * point here by id only ({@code concepts.folder_id}); there is deliberately no
 * JPA relationship, so the concept module never depends on this one.
 */
@Entity
@Table(name = "folders")
public class Folder {

    public static final int MAX_NAME_LENGTH = 120;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, length = 16)
    @Convert(converter = FolderColorConverter.class)
    private FolderColor color;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Folder() { }

    public Folder(UUID userId, String name, FolderColor color) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = normalizeName(name);
        this.color = color;
    }

    /** Trims and caps a name at {@link #MAX_NAME_LENGTH}; null or blank becomes "" (the caller decides whether that's allowed). */
    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() <= MAX_NAME_LENGTH ? trimmed : trimmed.substring(0, MAX_NAME_LENGTH).trim();
    }

    public void rename(String newName) {
        this.name = normalizeName(newName);
    }

    public void recolor(FolderColor newColor) {
        this.color = newColor;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public FolderColor getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
