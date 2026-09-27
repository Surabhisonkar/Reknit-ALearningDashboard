# Master Roadmap: Zero to Production

One document tracking every phase of this project, in build order, with
honest status. Update this as phases complete — it's the single source of
truth for "how far have we come" and "what's actually left."

Status key: ✅ Done &nbsp; 🟡 Partial &nbsp; ❌ Not started

---

## Phase 0 — Foundation ✅ DONE
Database schema (MySQL), Cognito auth + JIT user provisioning, ownership
enforcement on every endpoint, provider-neutral AI interfaces
(`TextGenerationProvider`/`VisualGenerationProvider`/`EmbeddingProvider`)
with retry/circuit-breaker/fallback chain, async SQS job orchestration
(`GenerationJob`, 202+poll pattern, separate api/worker services),
server-side RAG (embeddings + in-app cosine similarity).

## Phase 1 — Core AI Features ✅ DONE
Typed visualization payload model (mind map/diagram/animation/image) +
validator + renderer mapping. AI-decided animation structure (no
hardcoded scene template). Explain as a fully separate job/pipeline,
structured output (analogy + example sections required). OpenAI added to
the provider fallback chain. Asset lifecycle: S3 presigned upload/download
URLs, orphan-expiry for abandoned artifacts.

## Phase 2 — Frontend Migration & Structure ✅ DONE
Centralized API client, Cognito Hosted UI login (hand-rolled PKCE),
feature-folder architecture (`auth/`, `features/*`, thin `pages/`,
`shared/hooks`+`utils`+`ErrorBoundary`), duplicate-concept-name handling
(rename/replace/keep-both).

## Phase 3 — Security Hardening 🟡 MOSTLY DONE
- ✅ AuthZ (ownership checks everywhere), CORS lockdown, secrets via env
  only, Actuator locked to health/info, prompt-injection mitigation,
  content-safety validation on AI output, GDPR delete flow, backend +
  frontend dependency scanning in CI.
- 🟡 Secrets hygiene: `set-env.ps1` is now gitignored with a `set-env.example.ps1`
  template (2026-09-25). **Still on you:** rotate the Gemini API key that went
  out in an earlier zip.
- ❌ Privacy Policy / ToS (not code — a real doc, needed before real users)
- ❌ Logging-sanitizer audit (formal pass confirming no request bodies/
  tokens/PII ever reach a log line — done by habit so far, never audited)

## Phase 4 — Scaling Infra 🟡 PARTIAL, DELIBERATELY PAUSED
- ✅ Redis-backed distributed rate limiter, circuit breaker per AI
  provider, CI (build/test/lint/audit against real MySQL+Redis)
- ❌ Caching layer, Terraform/IaC, CD pipeline, real metrics/tracing
  instrumentation, load testing
- **Explicitly deferred** per your stated priority: features before
  scaling. Revisit only once the product phases below are further along.

## Phase 5 — Draft / Confirm-Save + Versioning ✅ VERIFIED DONE
Verified directly against the actual uploaded code (not assumed) —
implemented with a more sophisticated design than originally sketched:
a `JobHandler` registry replacing a switch statement, a proper
ports-and-adapters split (`ConceptDraftSource`/`ConceptArtifactLinker`/
`ConceptIndexer` owned by the `concept` module, implemented by adapters
elsewhere), and embedding indexing moved to a real async `INDEX_CONCEPT`
job fired after the save transaction commits, not an inline call. Full
class-level detail in `ARCHITECTURE_DIAGRAMS.md` Section 1.4/1.5/1.7.

**One real, known bug carried forward, not silently dropped**: if a
`@Transactional` call inside `JobProcessingService.process` throws (e.g.
the embedding provider fails), Spring marks the whole job-processing
transaction rollback-only — the job can't even be recorded as FAILED, so
SQS keeps redelivering it until the DLQ. Pre-existing, not introduced by
this phase, and the project's own verification doc names the real fix
(split "process" and "record outcome" into separate transactions) as a
deferred follow-up. **Tracked here so it doesn't get lost — pick this up
opportunistically, doesn't need its own phase.**

## Phase 6 — RAG Surfaced as a Real Feature 🟡 BUILT, WAITING ON THE STEP 5 PRODUCT CHECK
Delivered 2026-09-26 (steps 1–4 below): `GET /api/concepts/{id}/related` in
its own `RelatedConceptController`, `RetrievalService.findRelatedToConcept`,
`RelatedConceptResponse(id, title, summary, distance)`, and a "Connects to"
card row under the visualization on Workspace. The row doesn't render when
there's nothing related, or on error.

**Three decisions, confirmed by the user 2026-09-27:**
1. Own controller rather than a method on `ConceptController`.
2. The concept's **stored** embedding is the query. It is already
   `embed(title + ". " + summary)`, so it matches step 1's intent, and opening
   a concept never calls Gemini from the api process.
3. A configurable cut-off on top of top-k = 5: `RELATED_MAX_DISTANCE`
   (default 0.35, cosine distance) and `RELATED_TOP_K`. Without it,
   "related" would just mean "nearest".

**Done means step 5 has passed on real data** (it couldn't run in the
assistant's sandbox, where Gemini is blocked). Save the five concepts in
`learning-dashboard-frontend/scripts/related-concepts-fixtures.md`, then run
`node scripts/verify-related-concepts.mjs --token <token>`. It checks back
muscles ↔ posture both ways and keeps the controls out, and it prints a
measured `RELATED_MAX_DISTANCE` to replace the 0.35 guess.

## Phase 7 — Folders as Real Entities + Library Redesign 🟡 PARTIALLY MORE DONE THAN TRACKED
**Correction**: `GET /api/concepts/folders` already exists
(`ConceptService.listFoldersForUser`), returning distinct folder
*strings* for a basic filter dropdown. This is **not** the real Folder
entity design (no color, no rename, no per-user uniqueness) — that work
is unchanged and still needed — but it means a minimal folder filter
could ship on the frontend today, ahead of the full entity migration,
if that sequencing is ever useful.

## Phase 8 — Spark Feed: UI/Design Only 🟡 MORE BUILT THAN TRACKED (backend AND frontend)
**Correction 2026-09-27**: the frontend isn't a mockup either. `SparkPage`
plus `useSparkFeed` play the user's real saved *animation* concepts with
infinite scroll and interleave two working mini-games (`QuickMatchGame`,
`TapDashGame`) after every 3 cards. **Open question for the user:** are those
two games keepers or placeholders (see Phase 13)?

**Correction**: `GET /api/concepts/spark-feed?folder=&excludeIds=&limit=`
already exists (`ConceptService.randomSparkFeed`, capped at 25 per
request). This is only the "give me some concepts" data source for
Shuffle mode — nothing about teaser/reveal, gestures, mini-games, or the
other two modes is built, and the explicit working agreement (UI locked
before backend wiring) still applies to everything else in this phase.

**New product detail, captured 2026 — needs one design decision before
Phase 8 starts**: Spark feed cards should combine **animation + AI audio**
on reveal, not just a static visualization with audio playing separately.
Open question to resolve when this phase actually starts: does this mean
(a) the reveal *transition* itself should always be animated/motion-based
regardless of the underlying visualization type (an image gets a reveal
animation, a mind map's nodes animate into place, etc.), synchronized
with audio, or (b) Spark specifically favors/requires the `animation`
visualization type for cards, with audio narrating over it? These are
different UI/audio-sync designs — (a) is a Spark-feed presentation layer
concern; (b) would affect what `VisualizePromptBuilder` is asked to
generate. Not blocking now (Phase 8 hasn't started), but resolve before
locking the card UI, not during.

## Phase 9 — Spark Feed: Real Data ❌ NOT STARTED
Wire Phase 8's confirmed UI to real concepts. Requires new data: track
`last_reviewed_at` per concept (and ideally a lightweight review-history
table) to actually drive "due for review." Folder-deep-dive mode depends
on Phase 7's real folder entities existing.

## Phase 10 — Audio Narration Layer ❌ NOT STARTED
New provider interface mirroring the existing
`TextGenerationProvider`/`VisualGenerationProvider` shape. Must be
architected as one reusable service from day one — plugs into both
Concept Detail and Spark feed via the same implementation, never
duplicated per screen (explicit product requirement, not a suggestion).
No new storage entity needed — the existing `Artifact` table already
generalizes to any S3-backed file, an audio narration is just another
row with `content_type = audio/mpeg`. **See Phase 8's open question**:
whether Spark's "animation + AI audio" means a synchronized reveal
transition (an audio/animation-timing design concern for this phase) or
a bias toward the `animation` visualization type specifically — resolve
before this phase locks its interface shape, since the answer affects
whether `NarrationProvider` needs any timing/sync metadata at all.

## Phase 11 — (empty) RAG Surfaced moved to Phase 6. A stale duplicate "Phase 10 — RAG Surfaced" entry was removed 2026-09-27

## Phase 12 — Multi-Card Capture Splitting ❌ NOT STARTED
`VisualizePipeline` currently always produces exactly one concept per
call. The AI should be able to decide raw input actually contains
multiple distinct ideas and split it into linked cards instead of
forcing one. Meaningful prompt + pipeline redesign, not a small tweak.

## Phase 13 — Mini-Games, Streaks, Progress ❌ NOT STARTED, PARTLY UNDEFINED
- Mini-games: confirmed core to the product. Two games already exist in code
  (`QuickMatchGame`, `TapDashGame`, in `features/spark/games`, wired into the
  Spark feed), but they were never formally chosen. **Decide keep / replace
  before this phase starts**, not during it.
- Streaks/progress: confirmed to be "lightweight," no concrete mechanic
  chosen yet either.
- Due-for-review decay algorithm: confirmed automatic/time-based, exact
  formula still undecided (touches Phase 9 too).

## Phase 14 — Compliance Cleanup ❌ NOT STARTED
Privacy Policy/ToS (from Phase 3) actually written and live, footer links
in `Layout.jsx` (`/privacy`, `/terms` — currently dead) wired to real
pages. Logging-sanitizer audit actually performed, not just assumed.
Can happen in parallel with product phases whenever there's room — not
blocking, but genuinely needed before any real (non-you) user signs up.

## Phase 15 — Production Infrastructure ❌ NOT STARTED, DELIBERATELY PAUSED
Terraform/IaC for the full topology (ECS Fargate for api+worker, ALB, RDS
MySQL, ElastiCache, SQS+DLQ, S3, Secrets Manager, CloudWatch, Cognito),
CD pipeline, real observability (the Micrometer/CloudWatch dependencies
are already in the backend but nothing is instrumented), load testing,
a caching layer. **Do not start this until the product phases above are
substantially further along** — this was an explicit course-correction
earlier in the project (started too early once already).

## Phase 16 — Launch Readiness ❌ NOT STARTED
Final end-to-end QA across every phase above, a real beta/soft-launch
with actual other users (everything so far has been tested by you alone),
AWS spend caps confirmed on all three AI provider accounts if moving off
the free tier, monitoring/alerting actually wired to something you'll see
(not just collected), a real rollback plan.

---

## Where things stand (2026-09-27)

- **Phase 5 (A+B)**: delivered and verified in a browser against a mocked API. The backend compile and `mvn test` still need to be run on the user's machine.
- **Phase 6**: steps 1–4 delivered. **Step 5 (the real-data product check) is the next action, and it's the user's to run** (see Phase 6 above).
- **Cleanup release 2026-09-27**: 14 unused frontend files removed. The `AiProperties` embedding default was fixed to `gemini-embedding-001`, and `ARCHITECTURE_DIAGRAMS.md` was re-verified against the code. Current code: `learning-dashboard-release-2026-09-27.zip`.
- **Next phase: Phase 7, Folders + Library redesign**, using the corrected design below. Confirm scope with the user before writing any code.

## Decisions log
| Date | Decision |
|---|---|
| 2026-09-26 | **Standing rule:** confirm questions, scope and any deviation with the user *before* generating code. A better idea still gets asked, never just done. |
| 2026-09-27 | Related concepts: own controller; stored-embedding query with a configurable cut-off. |
| 2026-09-27 | `VisualizePipeline` (not `VisualizeJobHandler`) calls `VisualAssetGenerator`. The diagrams now match the code. |

## Corrected design for Phase 7 (Folders) — for when we get there

Recorded now so it doesn't get re-litigated later. Three gaps in the
original draft, now fixed:
1. **No dual-column limbo.** The migration backfills `Folder` rows from
   the existing `concepts.folder` strings, repoints `folder_id`, and
   **drops the old string column in the same migration** — not an
   indefinite "keep both for safety."
2. **`PATCH /api/folders/{id}`** included from the start (rename/recolor),
   matching the pattern already established for concepts.
3. **Case-insensitive unique constraint** on `(user_id, name)` — no
   accidental "Biology" vs "biology" duplicate folders.

