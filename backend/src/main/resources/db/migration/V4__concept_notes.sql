-- V4: notes saved on a concept (Phase 8). Today a note is an AI-condensed
-- Spark chat ("source" = 'chat'); the column leaves room for other kinds.
-- Deleting the concept or the user deletes its notes.
-- (The parked artifact-lifecycle migration moves to V5.)

CREATE TABLE concept_notes (
    id          CHAR(36) NOT NULL PRIMARY KEY,
    concept_id  CHAR(36) NOT NULL,
    user_id     CHAR(36) NOT NULL,
    content     TEXT NOT NULL,
    source      VARCHAR(16) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_concept_notes_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT fk_concept_notes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_concept_notes_concept_created ON concept_notes(concept_id, created_at);
