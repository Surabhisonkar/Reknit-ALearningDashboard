package com.learningdashboard.backend.user;

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
 * Local shadow of a Cognito identity. Every other table's ownership check
 * is a foreign key to this table's {@code id} — never to the Cognito
 * {@code sub} directly — so the rest of the schema isn't coupled to
 * Cognito internals if the auth provider ever changes.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "cognito_sub", nullable = false, unique = true)
    private String cognitoSub;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Set when the user requests account deletion (GDPR). Soft-delete grace window before purge. */
    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    protected User() { }

    public User(String cognitoSub, String email, String displayName) {
        this.id = UUID.randomUUID();
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.displayName = displayName;
    }

    public UUID getId() { return id; }
    public String getCognitoSub() { return cognitoSub; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletionRequestedAt() { return deletionRequestedAt; }
    public void setDeletionRequestedAt(Instant deletionRequestedAt) { this.deletionRequestedAt = deletionRequestedAt; }
}
