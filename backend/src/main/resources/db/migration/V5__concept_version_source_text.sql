-- The text each version's visual was generated from: the user's notes, or the
-- AI explanation if they chose to visualize that. Shown on the concept page
-- behind "Source notes". A user's own edit of the visual keeps the text of the
-- version it was made from.
ALTER TABLE concept_versions ADD COLUMN source_text MEDIUMTEXT NULL;

-- Backfill existing versions from the visualize job that produced them. Jobs
-- aren't linked to a version, only to the concept, so each version takes the
-- newest completed visualize job for its concept created before the version
-- (a capture's job comes before version 1, a regenerate's before the version
-- it appended). Versions with no such job keep NULL.
UPDATE concept_versions cv
SET cv.source_text = (
    SELECT JSON_UNQUOTE(JSON_EXTRACT(j.input_payload, '$.conceptText'))
    FROM generation_jobs j
    WHERE j.concept_id = cv.concept_id
      AND j.job_type = 'VISUALIZE'
      AND j.status = 'COMPLETED'
      AND j.created_at <= cv.created_at
    ORDER BY j.created_at DESC
    LIMIT 1
)
WHERE cv.source_text IS NULL;
