# Verifying Phase A + B (draft → confirm-save, concept versioning)

The backend could **not** be compiled in the assistant's sandbox (Maven Central is blocked there). What was checked there: every Java file parses, and a dependency-free `javac` type analysis found no errors in calls between the project's own classes (all remaining errors were missing Spring/JPA/Mockito/AssertJ classes). The first real compile is yours:

```powershell
mvn test            # 8 existing test classes + 5 new ones
mvn spring-boot:run # api profile
$env:SPRING_PROFILES_ACTIVE="worker"; mvn spring-boot:run   # worker, second terminal
```

**No migration runs for this change.** `V2__concept_versioning.sql` already existed and was already applied. Flyway should report the schema is up to date.

## Round trip with curl (bash; set `TOKEN` to a Cognito access token)

```bash
API=http://localhost:8080; H=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

# 1. Visualize -> a DRAFT, nothing saved
JOB=$(curl -s "${H[@]}" -X POST $API/api/jobs/visualize -d '{"conceptText":"Photosynthesis turns light into sugar"}' | jq -r .id)
curl -s "${H[@]}" $API/api/jobs/$JOB | jq '{status, conceptId, kind: .resultPayload.kind, title: .resultPayload.title}'
#    -> repeat until status COMPLETED; expect kind "DRAFT" and conceptId null
curl -s "${H[@]}" $API/api/concepts | jq length        # unchanged count

# 2. Confirm-save
curl -s -w '\n%{http_code}\n' "${H[@]}" -X POST $API/api/concepts -d "{\"jobId\":\"$JOB\"}"
#    201 + concept (currentVersion 1)
#    409 {"code":"DUPLICATE_TITLE","duplicateConceptId":...} if the title exists -> resend with
#        "title":"New name" | "onDuplicate":"REPLACE" | "onDuplicate":"KEEP_BOTH"
#    Saving the same jobId again -> 200 with the same concept (idempotent)
CID=<id from the 201>

# 3. The worker log shows an INDEX_CONCEPT job completing shortly after the save (embedding)

# 4. Post-save regenerate -> new version
RJ=$(curl -s "${H[@]}" -X POST $API/api/jobs/visualize -d "{\"conceptId\":\"$CID\"}" | jq -r .id)
curl -s "${H[@]}" $API/api/jobs/$RJ | jq '{status, conceptId, resultPayload}'   # kind VERSION, version 2

# 5. Versions
curl -s "${H[@]}" $API/api/concepts/$CID/versions | jq          # [{version:2,...},{version:1,...}]
curl -s "${H[@]}" $API/api/concepts/$CID/versions/1 | jq .title  # the original
```

## SQL spot checks

```sql
SELECT id, job_type, status, concept_id FROM generation_jobs ORDER BY created_at DESC LIMIT 5;
SELECT concept_id, version, title FROM concept_versions WHERE concept_id = '<CID>';
SELECT id, concept_id, concept_version_id, expires_at FROM artifacts ORDER BY created_at DESC LIMIT 5;
-- Unsaved drafts' artifacts: concept_id NULL and expires_at set (24h). Saved ones: both ids set, expires_at NULL.
```

## Known, not changed here

- **Existing rollback-only behaviour in the worker.** `JobProcessingService.process` runs a whole job in one transaction. If a `@Transactional` method it calls throws (for example `EmbeddingIndexService.indexConcept` when the embedding provider fails), Spring marks that transaction rollback-only. The job then can't be recorded as FAILED, and SQS redelivers it until the DLQ. This was already true before this change. The new code avoids adding more cases (the regenerate path uses non-proxied methods), but fixing it at the root means splitting "process" and "record outcome" into separate transactions. Worth a small follow-up.
- Drafts can be saved for 23h after they finish (their generated images expire at 24h). After that, Save returns `410 DRAFT_EXPIRED` and the UI shows the message. Regenerate to get a fresh draft.
