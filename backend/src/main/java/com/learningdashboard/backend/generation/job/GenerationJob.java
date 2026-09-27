package com.learningdashboard.backend.generation.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * The async job envelope. Every endpoint that needs an LLM creates one of
 * these (status PENDING), enqueues its id to SQS, and returns 202 — the
 * HTTP request never waits on a model call. Only {@code
 * GenerationJobWorker} (running in the worker ECS service) transitions
 * status away from PENDING.
 *
 * <p>JSON columns are kept as raw {@code String} (MySQL {@code json} type)
 * on the entity; parsing into typed payloads happens in the
 * service/pipeline layer, not here — this entity's only job is
 * persistence bookkeeping.
 */
@Entity
@Table(name = "generation_jobs")
public class GenerationJob {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "input_payload", columnDefinition = "json", nullable = false)
    private String inputPayload;

    @Column(name = "raw_model_response", columnDefinition = "mediumtext")
    private String rawModelResponse;

    @Column(name = "result_payload", columnDefinition = "json")
    private String resultPayload;

    @Column(name = "concept_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID conceptId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GenerationJob() { }

    public GenerationJob(UUID userId, JobType jobType, String inputPayload) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.jobType = jobType;
        this.inputPayload = inputPayload;
        this.status = JobStatus.PENDING;
    }

    public void markProcessing() {
        this.status = JobStatus.PROCESSING;
        this.attemptCount++;
    }

    public void markCompleted(String rawModelResponse, String resultPayload, UUID conceptId) {
        this.status = JobStatus.COMPLETED;
        this.rawModelResponse = rawModelResponse;
        this.resultPayload = resultPayload;
        this.conceptId = conceptId;
        this.errorMessage = null;
    }

    /**
     * Records which concept this job's draft was saved as (confirm-save).
     * Only ever called on an already-COMPLETED draft job; doubles as the
     * "already saved" marker that makes confirm-save idempotent.
     */
    public void linkConcept(UUID conceptId) {
        this.conceptId = conceptId;
    }

    public void markFailed(String errorMessage, String rawModelResponse) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.rawModelResponse = rawModelResponse;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public JobType getJobType() { return jobType; }
    public JobStatus getStatus() { return status; }
    public String getInputPayload() { return inputPayload; }
    public String getRawModelResponse() { return rawModelResponse; }
    public String getResultPayload() { return resultPayload; }
    public UUID getConceptId() { return conceptId; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
