# Design: Draft → Confirm-Save + Concept Versioning (Phase A + B)

Status: **implemented** (see "File map" at the bottom). This doc was written before the code, per the "design before code" standard in `CODEBASE_BASELINE.md`.

---

## 0. Goals and non-goals

**Goals**
1. A Visualize job produces a **draft**, not a saved concept. Nothing is written to `concepts` until the user clicks **Save**.
2. `POST /api/concepts` (confirm-save) promotes a draft into `concepts` + `concept_versions`, runs the duplicate-title check *before* writing, attaches artifacts, and triggers embedding indexing.
3. Post-save **Regenerate** (`POST /api/jobs/visualize` with `conceptId`) appends a real new version.
4. Capture shows the draft inline with Save / Regenerate / Discard. Concept Detail gets a ⋯ menu and version dots.

**Non-goals (unchanged, deliberately left alone)**
- Explain flow, auth, rate limiting, providers, RAG retrieval, Spark, Library, account/GDPR: all untouched.
- No new "drafts" table. The job row *is* the draft (Handoff Part 5).

---

## 1. Schema

Item 1 of the request (`concept_versions`, `concepts.current_version`, `artifacts.concept_version_id`) **already exists** as `V2__concept_versioning.sql` with its entities, and the backfill is already written. It is kept exactly as is. **No new migration is needed.** New job type values fit the existing `generation_jobs.job_type VARCHAR(32)`.

```mermaid
erDiagram
    users ||--o{ concepts : owns
    users ||--o{ generation_jobs : owns
    concepts ||--|{ concept_versions : "has 1..n"
    concepts ||--o{ artifacts : "attached to"
    concept_versions ||--o{ artifacts : "generated for"
    generation_jobs ||--o{ artifacts : "produced"
    generation_jobs }o--o| concepts : "promoted into / regenerated"
    concepts ||--o| concept_embeddings : "indexed as"

    concepts {
        char36 id PK
        char36 user_id FK
        varchar title "cache of current version"
        text summary "cache of current version"
        varchar folder "concept-level, never versioned"
        varchar visualization_type "cache"
        json visualization_payload "cache"
        int current_version "V2"
    }
    concept_versions {
        char36 id PK
        char36 concept_id FK
        int version "unique per concept, 1-based"
        varchar title
        text summary
        varchar visualization_type
        json visualization_payload
        datetime created_at
    }
    artifacts {
        char36 id PK
        char36 generation_job_id FK
        char36 concept_id FK "null while draft"
        char36 concept_version_id FK "null while draft (V2)"
        datetime expires_at "set while orphaned, cleared on attach"
    }
    generation_jobs {
        char36 id PK
        varchar job_type "EXPLAIN | VISUALIZE | INDEX_CONCEPT (new)"
        json input_payload
        json result_payload "DRAFT or VERSION result (see 3)"
        char36 concept_id "null for an unsaved draft, set on save"
    }
```

---

## 2. Class diagram

Dependency rule: **arrows point toward interfaces a module owns**. The `concept` module depends on nothing in `generation`, `storage` or `rag`. It declares three small ports, and the other modules implement them (Dependency Inversion).

```mermaid
classDiagram
    direction LR

    %% ---------------- concept module (domain) ----------------
    namespace concept {
        class ConceptDraft {
            <<record>>
            UUID generationJobId
            UUID sourceExplainJobId
            String title
            String summary
            String suggestedFolder
            String visualizationType
            String visualizationPayloadJson
            int payloadSchemaVersion
            List~UUID~ artifactIds
        }
        class DraftClaim {
            <<record>>
            ConceptDraft draft
            Optional~UUID~ savedConceptId
        }
        class ConceptDraftSource {
            <<interface>>
            +claim(jobId, userId) DraftClaim
            +markSaved(jobId, conceptId)
        }
        class ConceptArtifactLinker {
            <<interface>>
            +link(artifactIds, userId, conceptId, versionId)
        }
        class ConceptIndexer {
            <<interface>>
            +requestIndexing(conceptId, userId)
        }
        class ConceptVersionWriter {
            +write(concept, versionNumber, draft) ConceptVersion
        }
        class ConceptSaveService {
            +save(SaveConceptCommand) SaveOutcome
        }
        class ConceptRegenerationService {
            +appendVersion(conceptId, userId, draft) VersionAppended
        }
        class DuplicateTitlePolicy {
            <<enum>>
            REJECT
            KEEP_BOTH
            REPLACE
        }
        class ConceptService
        class ConceptRepository
        class ConceptVersionRepository
    }

    ConceptSaveService --> ConceptDraftSource
    ConceptSaveService --> ConceptVersionWriter
    ConceptSaveService --> ConceptRepository
    ConceptSaveService --> DuplicateTitlePolicy
    ConceptRegenerationService --> ConceptVersionWriter
    ConceptRegenerationService --> ConceptService
    ConceptVersionWriter --> ConceptVersionRepository
    ConceptVersionWriter --> ConceptRepository
    ConceptVersionWriter --> ConceptArtifactLinker
    ConceptVersionWriter --> ConceptIndexer
    ConceptDraftSource ..> DraftClaim
    DraftClaim --> ConceptDraft

    %% ---------------- generation module ----------------
    namespace generation {
        class VisualizePipeline {
            +run(userId, jobId, text, preferredType) Result
        }
        class VisualAssetGenerator {
            +attachAssets(payload, userId, jobId) AssetResult
        }
        class VisualizationDraft {
            <<record>>
            title, summary, suggestedFolder
            VisualizationPayload payload
            List~UUID~ artifactIds
        }
        class JobHandler {
            <<interface>>
            +type() JobType
            +handle(GenerationJob) JobResult
        }
        class JobProcessingService {
            -Map~JobType, JobHandler~ handlers
            +process(jobId)
        }
        class ExplainJobHandler
        class VisualizeJobHandler
        class VisualizationDraftCodec {
            +toDraftJson(draft) String
            +readDraft(job) ConceptDraft
            +toConceptDraft(draft, jobId, explainJobId) ConceptDraft
        }
        class JobConceptDraftSource
    }

    VisualizePipeline --> VisualAssetGenerator
    VisualizePipeline ..> VisualizationDraft
    JobProcessingService --> JobHandler
    ExplainJobHandler ..|> JobHandler
    VisualizeJobHandler ..|> JobHandler
    VisualizeJobHandler --> VisualizePipeline
    VisualizeJobHandler --> VisualizationDraftCodec
    VisualizeJobHandler --> ConceptRegenerationService : regenerate only
    JobConceptDraftSource ..|> ConceptDraftSource
    JobConceptDraftSource --> VisualizationDraftCodec

    %% ---------------- adapters in other modules ----------------
    class StorageConceptArtifactLinker
    class QueuedConceptIndexer
    class ConceptIndexJobHandler
    StorageConceptArtifactLinker ..|> ConceptArtifactLinker
    QueuedConceptIndexer ..|> ConceptIndexer
    ConceptIndexJobHandler ..|> JobHandler
```

### Why each class exists (SOLID)

| Class | Single responsibility | Principle it serves |
|---|---|---|
| `VisualizePipeline` | Turn text into a validated `VisualizationDraft`. **Persists nothing.** | SRP. Dependencies drop from 10 to 6, and it no longer depends on `concept` |
| `VisualAssetGenerator` | Generate + upload the image/scene assets a payload asks for (unattached, orphan TTL) | SRP. Extracted from the pipeline unchanged |
| `JobHandler` + registry in `JobProcessingService` | One handler per job type. The service keeps only lifecycle/failure bookkeeping | OCP: a new job type is a new class, with no `switch` to edit |
| `VisualizeJobHandler` | Choose *what to do* with a draft: store it as the job result (new) or append a version (regenerate) | SRP. Keeps the decision out of the pipeline |
| `VisualizationDraftCodec` | The **only** place that knows the draft JSON shape on `result_payload` | SRP / DRY. Written by the handler, read by the draft source |
| `ConceptDraftSource` (port) + `JobConceptDraftSource` | "Give me a saveable draft for this job, locked." The concept module never sees `GenerationJob` | DIP + ISP |
| `ConceptArtifactLinker` (port) + `StorageConceptArtifactLinker` | Attach artifacts to a concept version | DIP. The concept module doesn't know S3/artifact tables exist |
| `ConceptIndexer` (port) + `QueuedConceptIndexer` | "Make sure this concept gets (re-)embedded." Implemented by enqueueing an `INDEX_CONCEPT` job **after commit** | DIP. Also keeps the `api` process free of LLM/embedding calls (Handoff Part 4) |
| `ConceptVersionWriter` | Write one version: version row → link artifacts → refresh concept cache → request indexing | SRP, shared by save and regenerate so the two paths can't drift |
| `ConceptSaveService` | Confirm-save orchestration: idempotency, duplicate policy, create concept + v1, mark job saved | SRP |
| `ConceptRegenerationService` | Append version N+1 to an owned concept | SRP |
| `ConceptIndexJobHandler` (rag) | Worker side of indexing: load concept, embed, upsert | OCP (just another `JobHandler`) |

---

## 3. `result_payload` shapes (owned by `VisualizationDraftCodec`)

**New-concept Visualize job → DRAFT**
```json
{
  "kind": "DRAFT",
  "title": "The Zeigarnik effect",
  "summary": "…",
  "suggestedFolder": "Psychology",
  "visualizationType": "mind_map",
  "visualization": { "type": "mind_map", "version": 1, "...": "..." },
  "artifactIds": ["…uuid…"]
}
```
`job.conceptId` stays `null` until the draft is saved. Then it's set to the new concept's id, which also makes Save idempotent.

**Regenerate Visualize job (`conceptId` in input) → VERSION** (same keys as before, plus `kind`)
```json
{ "kind": "VERSION", "version": 3, "duplicateTitleConceptId": "…optional…" }
```
`job.conceptId` = the regenerated concept (unchanged behavior).

---

## 4. HTTP contract

### `POST /api/concepts` (new, confirm-save)
Request:
```json
{ "jobId": "uuid", "title": "optional override (rename)", "folder": "optional override", "onDuplicate": "REJECT | KEEP_BOTH | REPLACE" }
```
`onDuplicate` defaults to `REJECT`.

| Outcome | Status | Body |
|---|---|---|
| Saved | `201` | `ConceptResponse` |
| Draft already saved (double click, retry) | `200` | the already-saved `ConceptResponse` |
| Title collides and `onDuplicate=REJECT` | `409` | `{ "error", "code": "DUPLICATE_TITLE", "duplicateConceptId", "title" }` (**nothing written**) |
| Job isn't a completed draft (still running, failed, is a regenerate job) | `409` | `{ "error", "code": "DRAFT_NOT_READY" }` |
| Draft older than 23h (its assets' 24h orphan TTL is about to expire) | `410` | `{ "error", "code": "DRAFT_EXPIRED" }` |
| Not the caller's job / doesn't exist | `404` | `{ "error": "Not found." }` |

The duplicate modal's three choices map to: **Rename** → resend with `title`, **Replace** → resend with `onDuplicate=REPLACE` (the old concept is deleted in the *same* transaction, after the new one is written), **Keep both** → resend with `onDuplicate=KEEP_BOTH`.

### `POST /api/jobs/visualize` (existing, extended)
- Without `conceptId`: unchanged request. The result is now a DRAFT (see 3).
- With `conceptId` (post-save regenerate): `conceptText` becomes **optional**. When it's omitted, the server re-uses the input of the most recent Visualize job for that concept (text, preferred type, explain-job link). If there isn't one (a very old concept), it falls back to `title + summary`. `preferredVisualizationType` omitted → the original job's choice.

### Unchanged, already existed
`GET /api/concepts/{id}/versions`, `GET /api/concepts/{id}/versions/{version}`, `GET /api/jobs/{id}`, `GET /api/artifacts/{id}` (already scoped by owner only, so draft images render before save).

---

## 5. Sequences

### 5a. Capture: visualize → draft → save
```mermaid
sequenceDiagram
    actor U as User
    participant FE as CapturePage
    participant API as api profile
    participant Q as SQS
    participant W as worker profile
    U->>FE: Visualize
    FE->>API: POST /api/jobs/visualize {conceptText}
    API->>Q: enqueue(jobId)
    API-->>FE: 202 {jobId}
    W->>Q: poll
    W->>W: VisualizeJobHandler → VisualizePipeline (RAG, LLM, validate, assets uploaded as orphans)
    W->>W: job.result = DRAFT json, job.conceptId = null
    FE->>API: GET /api/jobs/{id} (poll) → DRAFT
    FE->>U: render draft inline (VisualizationRenderer)
    U->>FE: Save
    FE->>API: POST /api/concepts {jobId}
    API->>API: ConceptSaveService: lock job row → duplicate check
    alt title collides & REJECT
        API-->>FE: 409 DUPLICATE_TITLE (nothing written)
        FE->>U: modal: rename / replace / keep both → resend
    else ok
        API->>API: insert concept + version 1, attach artifacts, job.conceptId = id
        API->>Q: after commit: enqueue INDEX_CONCEPT
        API-->>FE: 201 concept
        FE->>U: navigate to /workspace?conceptId=…
    end
```
**Regenerate (pre-save)** = submit the same text again and replace the draft on screen. **Discard** = clear local state. The abandoned draft's artifacts keep their 24h orphan expiry, so no cleanup code is needed.

### 5b. Concept Detail: post-save regenerate
```mermaid
sequenceDiagram
    participant FE as WorkspacePage
    participant API
    participant W as worker
    FE->>API: POST /api/jobs/visualize {conceptId}
    API->>API: ownership check, resolve original input
    API-->>FE: 202 {jobId}
    W->>W: VisualizePipeline → draft
    W->>W: ConceptRegenerationService.appendVersion → ConceptVersionWriter (version N+1, attach artifacts, refresh cache, request indexing)
    FE->>API: poll job → VERSION {version:N+1}
    FE->>API: GET concept + GET versions → dots re-render
```

---

## 6. Frontend structure

```mermaid
classDiagram
    class CapturePage
    class WorkspacePage
    class useVisualizeFlow { +visualize(text) draft }
    class useSaveDraft { +save(jobId, options) }
    class useConceptVersions { +versions, selected, select(n), reload() }
    class useRegenerateConcept { +regenerate() }
    class DraftPreview { presentational }
    class DuplicateTitleModal { presentational }
    class OverflowMenu { shared/ui }
    class ProgressDots { shared/ui, extracted from AnimationRenderer }
    class VisualizationRenderer
    class draftMapper { domain }

    CapturePage --> useVisualizeFlow
    CapturePage --> useSaveDraft
    CapturePage --> DraftPreview
    CapturePage --> DuplicateTitleModal
    DraftPreview --> VisualizationRenderer
    useVisualizeFlow --> draftMapper
    WorkspacePage --> useConceptVersions
    WorkspacePage --> useRegenerateConcept
    WorkspacePage --> OverflowMenu
    WorkspacePage --> ProgressDots
    WorkspacePage --> VisualizationRenderer
```

- `ProgressDots` is the animation renderer's scene-dot markup and CSS classes (`animation-progress`, `animation-dot`, `active`), extracted so it produces **identical DOM**. The animation renderer and the version switcher both use it.
- Dots are hidden unless `currentVersion > 1`.
- `useConceptVersions` loads the list lazily only when there's more than one version, and fetches a historical version's payload only when its dot is clicked.

---

## 7. Behavior changes (intentional) versus preserved

| Area | Before | After |
|---|---|---|
| New Visualize | Saved a concept immediately, embedded inline in the worker | Returns a DRAFT, and saving is explicit. Embedding runs as an async `INDEX_CONCEPT` job after save |
| Duplicate title | Detected after save. "Replace" deleted the old concept, "rename" renamed the new one | Detected on Save **before** any write, with the same three choices |
| Regenerate via API | Appended a version and embedded inline | Still appends a version. Embedding moves to the async index job, and `conceptText` is optional |
| Everything else | — | Unchanged |

---

## 8. File map

**Backend: new**
`concept/ConceptDraft`, `concept/DraftClaim`, `concept/ConceptDraftSource`, `concept/ConceptArtifactLinker`, `concept/ConceptIndexer`, `concept/ConceptVersionWriter`, `concept/ConceptSaveService`, `concept/SaveConceptCommand`, `concept/SaveOutcome`, `concept/ConceptRegenerationService`, `concept/VersionAppended`, `concept/DuplicateTitlePolicy`, `concept/DuplicateTitleException`, `concept/DraftNotSaveableException`, `generation/model/VisualizationDraft`, `generation/pipeline/VisualAssetGenerator`, `generation/job/JobHandler`, `generation/job/JobResult`, `generation/job/ExplainJobHandler`, `generation/job/VisualizeJobHandler`, `generation/job/VisualizationDraftCodec`, `generation/job/JobConceptDraftSource`, `storage/StorageConceptArtifactLinker`, `rag/QueuedConceptIndexer`, `rag/ConceptIndexJobHandler`, `web/dto/ConceptSaveRequest`, and tests.

**Backend: changed**
`VisualizePipeline` (no persistence), `JobProcessingService` (registry), `JobType` (+`INDEX_CONCEPT`), `GenerationJob` (+`linkConcept`), `GenerationJobRepository` (+2 queries), `GenerationJobService` (+index/regenerate submit), `ConceptController` (+`POST`), `VisualizeController` + `VisualizeRequest` (optional text on regenerate), `GlobalExceptionHandler` (+2 mappings), `application.yml` (embedding model default fix).

**Frontend: new**
`api/conceptsApi` (+`saveConcept`, `listConceptVersions`, `getConceptVersion`), `api/jobsApi` (+`submitRegenerateJob`), `domain/draftMapper`, `features/capture/{hooks/useSaveDraft, components/DraftPreview, components/DuplicateTitleModal}`, `features/workspace/hooks/{useConceptVersions,useRegenerateConcept}`, `shared/ui/{ProgressDots,OverflowMenu}`, `shared/constants/visualizationTypes`.

**Frontend: changed**
`CapturePage`, `WorkspacePage`, `useVisualizeFlow` (returns the mapped draft), `AnimationRenderer` (uses `ProgressDots`, same DOM), `conceptMapper` (+`currentVersion`), `index.css` (draft + overflow styles).
