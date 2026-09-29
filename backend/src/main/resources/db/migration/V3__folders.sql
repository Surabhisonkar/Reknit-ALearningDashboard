-- V3: Folders become real entities (Phase 7).
--
-- Before: concepts.folder was a free-text string ('' = no folder).
-- After:  a per-user folders table (name + palette colour), and
--         concepts.folder_id -> folders.id (NULL = unfiled). The string
--         column is dropped at the end of this same migration.
--
-- Name rules (enforced by uk_folders_user_name):
--   * compared case-insensitively but accent-sensitively
--     ("Biology" = "biology", "Cafe" <> "Café"), via an explicit
--     utf8mb4_0900_as_ci collation on the column, so behaviour never
--     depends on the server's default collation;
--   * trailing/leading spaces are trimmed by the application; the backfill
--     trims too.
--
-- Backfill rules:
--   * names that differ only in letter case merge into one folder, keeping
--     the spelling used by the most concepts (tie: the spelling seen first);
--   * the backfill groups with the SAME collation as the unique key, so it
--     can never produce two rows that collide on it;
--   * colours are assigned round-robin through the palette, by name.
--
-- Safety: MySQL can't roll back DDL, so this migration is ordered so that
-- nothing is destroyed until the data has provably been carried over. The
-- temporary CHECK constraint below makes MySQL validate every concept row;
-- if any non-blank folder name failed to map, the migration stops there,
-- with concepts.folder still intact. Recovery steps: docs/VERIFY_PHASE_7.md.

-- 1. The folders table ------------------------------------------------------
CREATE TABLE IF NOT EXISTS folders (
    id          CHAR(36) NOT NULL PRIMARY KEY,
    user_id     CHAR(36) NOT NULL,
    name        VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
    color       VARCHAR(16) NOT NULL,          -- palette key: coral|yellow|teal|sky|violet|rose|green|slate
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT uk_folders_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_folders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 2. Backfill one folder per (user, case-insensitive name) -----------------
INSERT INTO folders (id, user_id, name, color, created_at, updated_at)
SELECT UUID(),
       ranked.user_id,
       ranked.name,
       ELT(1 + MOD(ranked.color_slot - 1, 8), 'coral', 'yellow', 'teal', 'sky', 'violet', 'rose', 'green', 'slate'),
       ranked.group_first_seen,
       UTC_TIMESTAMP(6)
FROM (
    SELECT picked.user_id,
           picked.name,
           picked.group_first_seen,
           ROW_NUMBER() OVER (PARTITION BY picked.user_id ORDER BY picked.name) AS color_slot
    FROM (
        SELECT spelled.user_id,
               spelled.name,
               ROW_NUMBER() OVER (
                   PARTITION BY spelled.user_id, spelled.name COLLATE utf8mb4_0900_as_ci
                   ORDER BY spelled.uses DESC, spelled.first_seen ASC, spelled.name COLLATE utf8mb4_bin ASC
               ) AS spelling_rank,
               MIN(spelled.first_seen) OVER (
                   PARTITION BY spelled.user_id, spelled.name COLLATE utf8mb4_0900_as_ci
               ) AS group_first_seen
        FROM (
            -- every exact spelling in use, with how many concepts use it
            SELECT user_id,
                   CONVERT(TRIM(folder) USING utf8mb4) COLLATE utf8mb4_bin AS name,
                   COUNT(*)        AS uses,
                   MIN(created_at) AS first_seen
            FROM concepts
            WHERE TRIM(folder) <> ''
            GROUP BY user_id, CONVERT(TRIM(folder) USING utf8mb4) COLLATE utf8mb4_bin
        ) spelled
    ) picked
    WHERE picked.spelling_rank = 1
) ranked
ON DUPLICATE KEY UPDATE id = folders.id;   -- re-running this step never creates duplicates

-- 3. Point every concept at its folder ------------------------------------
ALTER TABLE concepts ADD COLUMN folder_id CHAR(36) NULL AFTER summary;

UPDATE concepts c
JOIN folders f
  ON f.user_id = c.user_id
 AND f.name = CONVERT(TRIM(c.folder) USING utf8mb4) COLLATE utf8mb4_0900_as_ci
SET c.folder_id = f.id
WHERE TRIM(c.folder) <> '';

-- 4. Guard: MySQL validates existing rows when a CHECK is added. If any
--    concept with a non-blank folder name has no folder_id, this statement
--    fails and the migration stops here, before anything is dropped.
ALTER TABLE concepts
    ADD CONSTRAINT chk_v3_every_folder_mapped CHECK (TRIM(folder) = '' OR folder_id IS NOT NULL);
ALTER TABLE concepts DROP CHECK chk_v3_every_folder_mapped;

-- 5. Relationship + index for the new column ------------------------------
CREATE INDEX idx_concepts_user_folder_id ON concepts(user_id, folder_id);
ALTER TABLE concepts
    ADD CONSTRAINT fk_concepts_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL;

-- 6. Only now retire the string column ------------------------------------
DROP INDEX idx_concepts_user_folder ON concepts;
ALTER TABLE concepts DROP COLUMN folder;
