-- V2: Concept versioning. Every Visualize generation — including the very
-- first one for a concept — now writes a row to concept_versions. concepts
-- stops being the only copy of visualization content and becomes metadata
-- (user_id/folder, stable across regenerates) plus a denormalized cache of
-- the *current* version's content (title/summary/visualization_type/
-- visualization_payload/visualization_version), kept in sync on every
-- regenerate so GET /api/concepts/{id} and the Library list stay fast with
-- no join for the common case. concept_versions is what the version
-- switcher and GET /api/concepts/{id}/versions* read from.
--
-- Deliberately no "version 1 lives directly on concepts, version 2+ lives
-- elsewhere" special case: every version, including the first, gets its
-- own concept_versions row, and every artifact (including from the first
-- generation) points at the exact version it belongs to via
-- artifacts.concept_version_id. See VisualizePipeline for the two
-- persistence strategies (create-new-concept vs. append-new-version) that
-- both funnel through the same concept_versions write.

-- ---------------------------------------------------------------------
-- concept_versions: one row per generation. version is 1-based and
-- contiguous per concept (no gaps - versions are never deleted), so
-- concepts.current_version doubles as "how many versions exist" without
-- a COUNT(*) join.
-- ---------------------------------------------------------------------
CREATE TABLE concept_versions (
    id                      CHAR(36) NOT NULL PRIMARY KEY,
    concept_id              CHAR(36) NOT NULL,
    version                 INT NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    summary                 TEXT NOT NULL,
    visualization_type      VARCHAR(32) NOT NULL,
    visualization_payload   JSON NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    CONSTRAINT fk_concept_versions_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    UNIQUE KEY uq_concept_versions_concept_version (concept_id, version)
) ENGINE=InnoDB;

CREATE INDEX idx_concept_versions_concept_id ON concept_versions(concept_id);

-- ---------------------------------------------------------------------
-- concepts.current_version: the denormalized cache's version number.
-- Backfill first (every pre-existing concept becomes its own version 1
-- row below), then the column default of 1 is already correct for those
-- rows and for every concept created before this migration ran.
-- ---------------------------------------------------------------------
ALTER TABLE concepts ADD COLUMN current_version INT NOT NULL DEFAULT 1;

-- Backfill: every concept that already exists gets a version 1 row built
-- from its current (pre-versioning) content, so there's no "this concept
-- has no version history" gap for anything created before this migration.
INSERT INTO concept_versions (id, concept_id, version, title, summary, visualization_type, visualization_payload, created_at)
SELECT UUID(), id, 1, title, summary, visualization_type, visualization_payload, created_at
FROM concepts;

-- ---------------------------------------------------------------------
-- artifacts.concept_version_id: which exact version an artifact belongs
-- to. Nullable because it's new (a pre-existing artifact predates the
-- concept_versions table), but every pre-existing artifact attached to a
-- concept unambiguously belongs to that concept's backfilled version 1
-- row, so it's backfilled immediately below rather than left null.
-- ---------------------------------------------------------------------
ALTER TABLE artifacts ADD COLUMN concept_version_id CHAR(36);
ALTER TABLE artifacts ADD CONSTRAINT fk_artifacts_concept_version
    FOREIGN KEY (concept_version_id) REFERENCES concept_versions(id) ON DELETE SET NULL;
CREATE INDEX idx_artifacts_concept_version_id ON artifacts(concept_version_id);

UPDATE artifacts a
JOIN concept_versions cv ON cv.concept_id = a.concept_id AND cv.version = 1
SET a.concept_version_id = cv.id
WHERE a.concept_id IS NOT NULL;
