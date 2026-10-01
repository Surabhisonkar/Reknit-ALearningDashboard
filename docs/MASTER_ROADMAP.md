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
URLs. **Correction 2026-09-27: orphan expiry was never enforced.** `expires_at`
is written but nothing reads it, there's no cleanup job and no bucket rule in
the repo, and deleted concepts' and purged users' images stay in S3 (a GDPR
gap). The fix is designed in `design/artifact-lifecycle-cleanup.md`. **Parked
2026-09-28 so Phase 7 could go first**; its open questions are in
`docs/open-questions/artifact-lifecycle.md`, and its migration becomes V4.

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

## Phase 7 — Folders as Real Entities + Library Redesign ✅ DONE (verified and merged by the user, 2026-09-29)
Built 2026-09-28 on `feature/phase-7-folders`; design and decisions in
`backend/docs/design/phase-7-folders.md`. A new `folder` module (entity,
palette colour, case-insensitive/accent-sensitive unique names) behind ports
the `concept` module owns; `V3__folders.sql` backfills folders from the old
strings, repoints `concepts.folder_id` and drops the string column behind a
guard. New API: `/api/folders` (list with counts, create, rename/recolour,
delete with a required `?concepts=unfile|delete`), `PUT /api/concepts/{id}/folder`.
All existing concept routes keep their contract. Library redesign:
colour-coded folder row (one line + "N more" on desktop), search, move /
create / delete dialogs; shared `Modal`/`OverflowMenu` now manage keyboard focus.

**Done means:** the user's `mvn test` passes, V3 has been dry-run on a copy
of the database, and `frontend/scripts/verify-phase7-folders.mjs` passes on a
test account (`backend/docs/VERIFY_PHASE_7.md`).

## Phase 8 — Spark Feed + Ask the AI + Notes 🟡 BUILT, WAITING ON THE USER'S RUN
Built 2026-09-29 on `feature/phase-8-spark` in three steps (UI locked on a
mock, backend, wiring). Design and every decision: `backend/docs/design/phase-8-spark.md`.
- **Spark**: one card on stage, teaser then tap to reveal (a per-type entrance),
  endless feed (user decision, replacing "short sessions") in the mix games 20 /
  animation 60 / mind map 15 / image + diagram 5, swipes / wheel / keys, related
  sub-stack, icon-only deep dive, Due for review says "coming soon".
- **Ask the AI**: a chat in a bottom sheet (worker jobs `ASK_CONCEPT`,
  `CHAT_TO_NOTE`); save as an AI-condensed note (new `concept_notes`, V4, shown
  on Concept Detail) or make a visual via Capture.
- **Foundations**: a swappable `Narrator` (browser voice today), a `GameRegistry`
  + `GamePicker` strategy, a `SparkFeedSampler` with weights in `app.spark.mix`.

**Done means:** the user's `mvn test` passes and
`frontend/scripts/verify-phase8-spark.mjs` passes on a test account
(`backend/docs/VERIFY_PHASE_8.md`).

## Phase 9 — Spark Feed: Review Tracking ❌ NOT STARTED
*Renamed 2026-09-29: Phase 8 already runs Shuffle and Folder mode on real data. What's left is "Due for review".*
Wire Phase 8's confirmed UI to real concepts. Requires new data: track
`last_reviewed_at` per concept (and ideally a lightweight review-history
table) to actually drive "due for review." Folder-deep-dive mode depends
on Phase 7's real folder entities existing.

## Phase 10 — (moved) AI voice-over is now the LAST phase, after Phase 16
User decision 2026-09-29. The foundation is already built in Phase 8: the
card's narration controls and a swappable `Narrator` interface
(`frontend/src/shared/narration/`), used today by a browser-voice
implementation. The AI voice becomes one more `Narrator` implementation (it
plays a narration artifact from the backend) plus a backend
`NarrationProvider` behind the usual provider interface, with the audio stored
as an ordinary `Artifact` row (`content_type = audio/mpeg`) - no new table.
The Phase 8 open question is resolved: every visual type appears in Spark and
the reveal itself is always animated, so narration needs no per-type timing.

## Phase 11 — (empty) RAG Surfaced moved to Phase 6. A stale duplicate "Phase 10 — RAG Surfaced" entry was removed 2026-09-27

## Phase 12 — Multi-Card Capture Splitting ❌ NOT STARTED
`VisualizePipeline` currently always produces exactly one concept per
call. The AI should be able to decide raw input actually contains
multiple distinct ideas and split it into linked cards instead of
forcing one. Meaningful prompt + pipeline redesign, not a small tweak.

## Phase 13 — Mini-Games, Streaks, Progress ❌ NOT STARTED, PARTLY UNDEFINED
- Mini-games: confirmed core to the product. Phase 8 kept `QuickMatchGame` and
  `TapDashGame` as **placeholders** (user decision) behind a `GameRegistry` +
  `GamePicker` strategy: a new game is one component plus one `register(...)`
  line in `features/spark/games/catalog.js`. **Choose the real games before this
  phase starts.**
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

## Where things stand (2026-09-29)

- **Phases 5, 6 (steps 1–4) and 7** are merged. Phase 6 step 5 (the real-data product check) is still the user's to run.
- **Phase 8 (Spark + Ask the AI + Notes)**: built on `feature/phase-8-spark`. Next action is the user's: `backend/docs/VERIFY_PHASE_8.md`.
- **Pending**: the artifact-lifecycle fix (its open questions are in the user's local `docs/open-questions/`; its migration is now V5).

## Decisions log
| Date | Decision |
|---|---|
| 2026-09-26 | **Standing rule:** confirm questions, scope and any deviation with the user *before* generating code. A better idea still gets asked, never just done. |
| 2026-09-27 | Related concepts: own controller; stored-embedding query with a configurable cut-off. |
| 2026-09-27 | `VisualizePipeline` (not `VisualizeJobHandler`) calls `VisualAssetGenerator`. The diagrams now match the code. |
| 2026-09-28 | Phase 7 goes before the artifact-lifecycle fix; the lifecycle's open questions are parked in `docs/open-questions/`. |
| 2026-09-28 | Phase 7 decisions (module, auto-filing, colours, name collation, API compatibility, delete modes, UI): see `backend/docs/design/phase-7-folders.md`. |
| 2026-09-29 | Phase 8 decisions (reveal, mix 20/60/15/5, endless feed, narrator foundation with the AI voice moved last, game registry, Ask the AI with notes/visual, coming-soon Due, teaser on return, finger-scroll visuals, sampler, ask rate limit 20/min, prompt tag neutralizing): `backend/docs/design/phase-8-spark.md`. |
| 2026-09-29 | `docs/open-questions/` stays local (git-ignored by the user); patches no longer include it. |
| 2026-09-28 | Deliveries are a single git patch against `main` plus a list of changed files, not a full zip. Each phase is its own branch, tested by the user, then merged. Questions come as multiple choice mid-work, then work resumes. |
| 2026-09-27 | Artifact lifecycle: an app sweeper plus an S3 tag/bucket-rule backstop (7 days), GDPR erasure in the same change, a dry-run reconcile, and `AccountPurgeJob` worker-only. Monorepo `backend/` + `frontend/`, CI moves to the root `.github/`. Extras deferred to `TECH_DEBT_TASKS.md` (T1–T5). |

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

