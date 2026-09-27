-- V1: Foundation schema, MySQL (RDS). Owned entirely by Flyway; Hibernate
-- ddl-auto is set to "validate" so this file is the single source of truth.
--
-- UUID columns are CHAR(36), always assigned by the application
-- (java.util.UUID.randomUUID() in each entity's constructor) rather than a
-- database-generated default - no MySQL equivalent of pgcrypto's
-- gen_random_uuid() is needed. Every entity's Java @Id field is annotated
-- @JdbcTypeCode(SqlTypes.CHAR) so Hibernate binds/reads UUIDs as this exact
-- CHAR(36) representation rather than relying on a dialect default.

-- ---------------------------------------------------------------------
-- users: local shadow of Cognito identities, JIT-provisioned on first
-- authenticated request (see CognitoUserProvisioningFilter). Every other
-- table's ownership check is a foreign key to this table's id, never to
-- the Cognito "sub" directly.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                     CHAR(36) NOT NULL PRIMARY KEY,
    cognito_sub            VARCHAR(255) NOT NULL,
    email                  VARCHAR(320) NOT NULL,
    display_name           VARCHAR(255),
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    -- GDPR: soft-delete first (grace period before hard purge). See
    -- DataDeletionService / AccountPurgeJob.
    deletion_requested_at  DATETIME(6),
    UNIQUE KEY uq_users_cognito_sub (cognito_sub)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- concepts: server-side persistence replacing the old browser
-- localStorage store entirely. visualization_payload is the normalized,
-- validated, versioned payload (see generation.model) - never raw model
-- output; that lives only on the generation_jobs row that produced it.
-- ---------------------------------------------------------------------
CREATE TABLE concepts (
    id                      CHAR(36) NOT NULL PRIMARY KEY,
    user_id                 CHAR(36) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    summary                 TEXT NOT NULL,
    folder                  VARCHAR(120) NOT NULL DEFAULT '',
    visualization_type      VARCHAR(32) NOT NULL,      -- mind_map | diagram | animation | image
    visualization_payload   JSON NOT NULL,
    visualization_version   INT NOT NULL DEFAULT 1,
    source_explain_job_id   CHAR(36),
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    CONSTRAINT fk_concepts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_concepts_user_id ON concepts(user_id);
CREATE INDEX idx_concepts_user_folder ON concepts(user_id, folder);

-- ---------------------------------------------------------------------
-- generation_jobs: the async job envelope. Every LLM-touching request
-- creates one of these and returns 202; the worker service is the only
-- thing that transitions status away from PENDING.
-- ---------------------------------------------------------------------
CREATE TABLE generation_jobs (
    id                  CHAR(36) NOT NULL PRIMARY KEY,
    user_id             CHAR(36) NOT NULL,
    job_type            VARCHAR(32) NOT NULL,            -- EXPLAIN | VISUALIZE
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_payload       JSON NOT NULL,
    raw_model_response  MEDIUMTEXT,                       -- audit trail: exactly what the provider returned
    result_payload      JSON,                              -- normalized result once COMPLETED
    concept_id          CHAR(36),
    error_message       VARCHAR(1000),
    attempt_count       INT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    CONSTRAINT fk_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_generation_jobs_user_id ON generation_jobs(user_id);
CREATE INDEX idx_generation_jobs_status ON generation_jobs(status);

-- ---------------------------------------------------------------------
-- artifacts: lifecycle-tracked S3 objects (generated images today; video
-- frames / narration audio later). expires_at drives an S3 lifecycle
-- rule plus a periodic cleanup job for orphaned rows.
-- ---------------------------------------------------------------------
CREATE TABLE artifacts (
    id                  CHAR(36) NOT NULL PRIMARY KEY,
    user_id             CHAR(36) NOT NULL,
    generation_job_id   CHAR(36),
    concept_id          CHAR(36),
    s3_key              VARCHAR(1024) NOT NULL,
    content_type        VARCHAR(255) NOT NULL,
    size_bytes          BIGINT,
    version             INT NOT NULL DEFAULT 1,
    created_at          DATETIME(6) NOT NULL,
    expires_at          DATETIME(6),
    CONSTRAINT fk_artifacts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_artifacts_job FOREIGN KEY (generation_job_id) REFERENCES generation_jobs(id) ON DELETE SET NULL,
    CONSTRAINT fk_artifacts_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_artifacts_concept_id ON artifacts(concept_id);
CREATE INDEX idx_artifacts_expires_at ON artifacts(expires_at);

-- ---------------------------------------------------------------------
-- concept_embeddings: server-side RAG. No native vector type in MySQL
-- (unlike Postgres+pgvector), so the embedding is stored as a JSON
-- array of floats and similarity search runs in the application layer
-- (see rag.RetrievalService / rag.CosineSimilarity) rather than as a
-- database operator. This is a deliberate, documented scaling trade-off
-- - fine at the per-user concept counts this product expects; if that
-- ever changes, RetrievalService's interface (embed query, return top-k
-- RelatedConcept) is what a future OpenSearch-backed implementation
-- would satisfy instead, with no caller changes.
-- ---------------------------------------------------------------------
CREATE TABLE concept_embeddings (
    id              CHAR(36) NOT NULL PRIMARY KEY,
    concept_id      CHAR(36) NOT NULL,
    user_id         CHAR(36) NOT NULL,
    embedding       JSON NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_embeddings_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT fk_embeddings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_embeddings_concept (concept_id)
) ENGINE=InnoDB;

CREATE INDEX idx_concept_embeddings_user_id ON concept_embeddings(user_id);
