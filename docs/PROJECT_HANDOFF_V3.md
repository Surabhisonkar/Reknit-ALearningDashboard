# Project Handoff v3 — read this first

**As of 2026-09-27. Supersedes `PROJECT_HANDOFF_V2.md`.** V2 is kept in the project for history, but everything still true in it is repeated here. When this document and your own assumptions disagree, this document wins. When this document and the **code** disagree, the code wins: say so, and ask the user before acting.

Companion documents (all in the Claude Project, and also in the zip's `docs/` folder):

| Document | What it is | Authority |
|---|---|---|
| `MASTER_ROADMAP.md` | Phase order, status, decisions log | **User's source of truth** for what to build and when |
| `ARCHITECTURE_DIAGRAMS.md` | Class diagrams per module, ER diagram, use cases. Re-verified against the code 2026-09-27 | **User's source of truth** for design; new work must follow it |
| `CODEBASE_BASELINE.md` | Engineering standards (binding) and the known tech-debt list | Binding rules |
| `design/phase-a-b-draft-save-and-versioning.md`, `design/phase-f-related-concepts.md` | Per-phase designs written before the code | Reference |

---

## 0. The rules of working with this user (non-negotiable)

1. **Confirm before code.** Before generating *any* code, ask questions and confirm scope, approach, and every deviation from the roadmap or diagrams with the user, using the question tool. Wait for explicit answers. A deviation you're sure is better still gets **asked**, never done and explained afterwards. (Set 2026-09-26, after an assistant built a better-but-unapproved design.)
2. **Design before code.** Any new or changed class, module or table starts with a class diagram and/or ER diagram (Mermaid) that the user reviews. Validate that the Mermaid renders (a `{...}` inside a class body breaks the parser).
3. **SOLID and low coupling, concretely** (see `CODEBASE_BASELINE.md` §5):
   - Modules talk through interfaces they own (ports), never another module's repository.
   - Use registries instead of `switch` for anything type-keyed.
   - One responsibility per class; thin controllers.
4. **Don't change working behaviour** unless the user asked. List every file you change.
5. **Verification honesty:**
   - **Frontend:** run `npm run lint` and `npm run build` before delivering. Headless-browser tests with Playwright against a mocked API are possible and have been used.
   - **Backend:** cannot be compiled in the assistant's sandbox (Maven Central → 403 by policy). Every time, state that the user's `mvn test` output is the real check.
   - **Partial checks that do work:** a `javac` parse, and `javac` analysis with library-missing errors filtered out, which catches errors in calls between the project's own classes.
   - Gemini and Hugging Face are also blocked from the sandbox, so no real embeddings or LLM calls are possible there.
6. **Real logs beat code review.** When the user pastes a real error or log, treat it as ground truth.
7. **Deliverables:** a full zip for multi-file changes; just the changed file for a tiny fix. Never ship `node_modules`, `target`, `dist`, or `set-env.ps1`.
8. **UI is locked page by page before backend wiring** (the user's working agreement). **No infra/scaling work** (Terraform, CD, caching, load tests) until the product phases are further along.

---

## 1. What the product is

**Not** a note-taking app, blog, or documentation tool. It **is** a *visual memory tool for an ADHD brain* that struggles to retain information by re-reading text. The effort of reading happens **once, at capture**. Revisiting means **looking, not reading**, and it must work on a low-energy day.

**Core loop:**
1. **Capture:** dump raw notes, or have the AI explain a topic.
2. **Transform:** the AI makes a visual (mind map, diagram, animation or image).
3. **Revisit:** look at it later, reels-style.

The product is domain-agnostic (anything the user wants to retain).

**Anti-goals:** every screen resists adding text. There is no long-form content.

**Emotional core:** revisiting should feel like scrolling short-form video, not opening a textbook.

**ADHD UX principles:**
- Short sessions with a clear end ("you've reviewed 5 today").
- Big touch targets.
- Motion only as reward or feedback, never decoration.
- Colour *categorises* (each folder or topic gets a consistent colour).
- Dark mode alone is not enough.

**Aesthetic:** minimal but not boring, colourful but not cluttered.

**Folders:** created at any time, unstructured, "like a YouTube playlist". Never a forced hierarchy.

**The AI's two product jobs:**
- **RAG** makes the library associative. New concepts quietly connect to related older ones, even across folders ("back muscles" ↔ "posture").
- **The agent** removes structuring effort at capture. It decides the core idea, the best visual type, and whether to split the input into several linked cards (not built).

**Spark feed** (the most novel screen):
- **Three modes:**
  - Shuffle.
  - Folder deep-dive.
  - Due-for-review: automatic and time-based, with **no** self-rating.
- **Card loop:** the teaser (title only) shows first → tap to reveal the visual, with AI audio that autoplays.
- **Gestures:**

  | Gesture | Action |
  |---|---|
  | Tap | Reveal (concept card) or play (game card) |
  | Swipe up / down | Next / previous card |
  | Swipe left / right | Into / out of a sub-stack of related concepts |

- **Deep-dive** is an icon, not a gesture: "ask the AI" or "open in Library".
- **Mini-games** are a core feature: dopamine resets mixed randomly between cards. They are not quizzes.
- **Audio narration** must be **one reusable layer** used by Concept Detail and Spark, never duplicated per screen.
- **Progress** is a light touch ("reviewed 5 today").

New detail (roadmap Phase 8): Spark cards combine **animation + AI audio** on reveal. One question is still open, to resolve before Phase 8: (a) the reveal is always animated, whatever the visual type, or (b) Spark favours the `animation` visual type.

**Screens:** Landing, About, Library (folders + search), Capture, Concept Detail (`/workspace`), Spark. Build Spark last among them.

---

## 2. Tech stack (as running)

**Backend:** Spring Boot 3.5.9, Java 21, Maven.

- **Database:** MySQL 8, with Flyway migrations `V1__init_schema.sql`, `V2__concept_versioning.sql` and `V3__folders.sql` (Phase 7). Hibernate `ddl-auto: validate`.
- **Auth:** Cognito (Hosted UI + PKCE). JWT resource server. Users are provisioned just in time by `CognitoUserProvisioningFilter`, which delegates to `UserProvisioningService`.
- **Async:** AWS SQS. One jar, two profiles:
  - `api`: controllers only, **never calls an AI provider**.
  - `worker` (`SPRING_PROFILES_ACTIVE=worker`): long-polls SQS and is the **only** place AI providers are called.
- **Storage:** S3 with presigned URLs. Generated assets start as orphans with a 24h `expires_at` and are attached on save. **But nothing enforces the expiry yet**; see §4 and `design/artifact-lifecycle-cleanup.md`.
- **Rate limiting:** Redis (Memurai on the user's Windows machine) + Bucket4j, keyed by user id.
- **Resilience:** Resilience4j per provider (retry 1s→2s→4s, 4 attempts; circuit breaker) plus a fallback chain.
- **AI:** Gemini (active, free tier), Claude and OpenAI (keys exist, not in the active order). They sit behind `TextGenerationProvider` / `VisualGenerationProvider` / `EmbeddingProvider`, with `*_PROVIDER_ORDER` env vars.
  - Current models: `gemini-3.6-flash` (text), `gemini-2.5-flash-image` (visual), `gemini-embedding-001` (embedding).
  - Check https://ai.google.dev/gemini-api/docs/deprecations before assuming any model name still works.
- **CI:** GitHub Actions (build + test against MySQL/Redis service containers, OWASP dependency-check).

**Frontend:** React 19 + Vite 8 + Tailwind 4, react-router 7, plain `fetch` API client. Cognito PKCE is hand-rolled. The session lives in `sessionStorage["auth_session"]`. There's no client-side AI or RAG.

- **Layers:** `api/` → `domain/` (mappers; renderers never see raw JSON) → `features/*` (hooks + components) → `pages/` (thin). Plus `shared/{ui,hooks,constants,utils}` and `auth/{api,context,hooks,storage,utils}`.
- **Routes:** `/`, `/about`, `/auth/callback`, and `/spark`, `/library`, `/workspace?conceptId=`, `/create`. The last four require auth.

**Free-to-run constraint:** local MySQL and Redis, plus free tiers of Cognito, SQS, S3 and Gemini.

**Local setup:** env vars come from `set-env.ps1`, which is gitignored. Copy `set-env.example.ps1` to create it. The frontend uses `.env.local` (see `.env.example`).

---

## 3. What is built, phase by phase

| Phase | Status | Notes |
|---|---|---|
| 0–2 Foundation, core AI, frontend structure | ✅ | |
| 3 Security | 🟡 | Missing: Privacy Policy/ToS and a logging-sanitizer audit. **The user must rotate the Gemini key that leaked in an early zip.** |
| 4 Scaling infra | 🟡 paused | Deliberately |
| 5 Draft → confirm-save + versioning (A+B) | ✅ in code; browser-tested against a mocked API | Awaiting the user's `mvn test` and a real run |
| 6 Related concepts (RAG surfaced) | 🟡 steps 1–4 built | **Step 5, the real-data product check, is the user's next action** (§6) |
| 7 Folders + Library redesign | 🟡 built on `feature/phase-7-folders` | Awaiting the user's `mvn test`, V3 dry run and test-account check (`backend/docs/VERIFY_PHASE_7.md`) |
| 8 Spark UI | 🟡 more exists than planned | A real feed of saved *animation* concepts + 2 mini-games; none of the Spark design itself |
| 9–16 | ❌ | See the roadmap |

### API (complete list, from the code)

```
GET    /api/account                      PUT /api/account/profile
DELETE /api/account                      POST /api/account/cancel-deletion
GET    /api/artifacts/{id}               -> presigned download URL (owner-checked; works for draft assets)
POST   /api/jobs/explain                 -> 202 job
POST   /api/jobs/visualize               -> 202 job. No conceptId: result is a DRAFT. With conceptId: post-save
                                            regenerate, appends version N+1; conceptText optional (reuses original input)
GET    /api/jobs/{id}                    -> poll. resultPayload.kind = "DRAFT" | "VERSION" for visualize jobs
GET    /api/concepts?folder=             POST /api/concepts  (confirm-save {jobId,title?,folder?,onDuplicate?};
                                            201 new, 200 already saved, 409 DUPLICATE_TITLE/DRAFT_NOT_READY, 410 DRAFT_EXPIRED)
GET    /api/concepts/{id}                PATCH /api/concepts/{id} (rename)     DELETE /api/concepts/{id}
GET    /api/concepts/{id}/versions       GET /api/concepts/{id}/versions/{version}
GET    /api/concepts/{id}/related?limit=&maxDistance=   -> [{id,title,summary,distance}] (stored-vector, no AI call)
GET    /api/concepts/folders             -> names of folders holding >= 1 concept (legacy, for Spark)
PUT    /api/concepts/{id}/folder         -> move {folderId | null}; 404 if the concept or folder isn't yours
GET    /api/folders                      POST /api/folders {name, color?}   PATCH /api/folders/{id} {name?, color?}
DELETE /api/folders/{id}?concepts=unfile|delete   (parameter required; 400 otherwise)
GET    /api/concepts/spark-feed?folder=&excludeIds=&limit=   -> random animation concepts
```

### Key backend design (details in `ARCHITECTURE_DIAGRAMS.md`)

- **Job processing:** `JobProcessingService` dispatches to one `JobHandler` bean per `JobType`: `ExplainJobHandler`, `VisualizeJobHandler`, `ConceptIndexJobHandler`. **A new job type = a new handler bean.**
- **Visualize:** `VisualizePipeline` *persists nothing*. It does RAG, the LLM call, validation, and calls `VisualAssetGenerator` (images uploaded as orphans), then returns a `VisualizationDraft`. `VisualizeJobHandler` then decides: store the draft as the job result (`VisualizationDraftCodec` owns that JSON), or append a version via `ConceptRegenerationService`.
- **Concept module ports:** `concept` depends on no other module. It owns three ports: `ConceptDraftSource`, `ConceptArtifactLinker` and `ConceptIndexer`. Their adapters are `JobConceptDraftSource`, `StorageConceptArtifactLinker` and `QueuedConceptIndexer`.
- **Writing versions:** `ConceptVersionWriter` is the single path for writing a version. Both `ConceptSaveService` (confirm-save) and regenerate use it.
- **Embedding:** it runs as an async `INDEX_CONCEPT` job, enqueued **after commit**.
- **Related concepts:** they use the stored embedding plus `RELATED_MAX_DISTANCE` (0.35 default) and `RELATED_TOP_K` (5).

### Tests

20 backend unit-test classes (8 original + 5 for Phase A+B + 1 for Phase 6 + 6 for Phase 7). For the frontend there are no unit tests; checking is lint + build + Playwright scripts.

---

## 4. Known issues and tech debt (don't rediscover these)

0. **Artifact lifecycle not enforced (found 2026-09-27, highest priority).** Orphaned draft images, deleted concepts' images and purged users' images are never removed from S3, which is a storage leak and a GDPR erasure gap. The fix is fully designed with the user's decisions recorded (`design/artifact-lifecycle-cleanup.md` §7). It's waiting on the user's go-ahead and on the output of their `aws s3api get-bucket-lifecycle-configuration`. Coupling and cleanup extras are in `TECH_DEBT_TASKS.md` (T1–T5).
1. **The worker's rollback-only trap (pre-existing):** `JobProcessingService.process` runs a whole job in one transaction. If a `@Transactional` method called inside it throws (for example, embedding failures in `EmbeddingIndexService.indexConcept`), the transaction becomes rollback-only. The job can't then be marked FAILED, and SQS redelivers it until the DLQ.
   - **Fix:** separate transactions for "process" and "record outcome". **Pick this up opportunistically.**
   - New code avoids adding cases: methods on the regenerate path are deliberately *not* `@Transactional`.
2. The remaining SOLID hotspots from `CODEBASE_BASELINE.md` §4, still open:
   - A `VisualPayloadValidator` switch.
   - `visualizationType` stored as a raw String.
   - Rate limiting inline in controllers.
   - `ConceptController` serves Spark and folders.
   - The frontend renderers fetch artifact URLs themselves.
   - `accessToken` threaded through every API call.
   - `TYPE_LABELS` still duplicated in `LibraryPage`.
   - Inline styles.
3. `ForbiddenException` is unused (ownership failures map to 404). Remove it or wire it in.
4. Pre-existing Workspace header styling: the title is small, and it touches the left edge on mobile.

## 5. Hard-won lessons (all still true)

1. Never put `@Transactional` on a Servlet `Filter` (CGLIB/Objenesis → NPE). Delegate to a `@Service`.
2. `@Lazy` on a `@Bean` does nothing unless the *injection point* is `@Lazy` too.
3. React StrictMode double effects: use a `hasRun` ref for one-shot work (OAuth code exchange). For idempotent fetches, a `cancelled` flag is fine.
4. A React state update right before `window.location.assign()` can race.
5. Cognito *access* tokens lack `email`/`name`. Those are on the ID token.
6. Spring Boot 3.5 `ClientHttpRequestFactoryBuilder` lives in `org.springframework.boot.http.client`.
7. You need `resilience4j-all` for `Decorators`. Bucket4j's artifact is `bucket4j-core`.
8. Gemini model names churn: `text-embedding-004` died 2026-01-14. Free-tier Gemini 503s under load need retry backoff measured in seconds.
9. Exceptions thrown through a *joined* `@Transactional` proxy poison the outer transaction (see §4.1).
10. Mermaid: `{...}` inside a class body breaks the parser. Write `:id`.

## 6. What happens next (in order)

1. **User: run the backend for real:**
   - `mvn test` (14 test classes)
   - `mvn spring-boot:run` (api)
   - `mvn spring-boot:run` again with `SPRING_PROFILES_ACTIVE=worker`

   Then walk through `learning-dashboard-backend/docs/VERIFY_PHASE_A_B.md` and `VERIFY_PHASE_F.md`. If anything fails, paste the real error; fix from that.
2. **User: Phase 6 step 5.** Save the five concepts in `learning-dashboard-frontend/scripts/related-concepts-fixtures.md`, then run `node scripts/verify-related-concepts.mjs --token <token>`. Set the suggested `RELATED_MAX_DISTANCE`.
3. **User: Phase 7.** Follow `backend/docs/VERIFY_PHASE_7.md`: dry-run `V3__folders.sql` on a copy of the database, `mvn test`, then `frontend/scripts/verify-phase7-folders.mjs` on a separate test account. Merge `feature/phase-7-folders` when it passes.
4. **Then the artifact-lifecycle fix** (§4 item 0), resumed from `docs/open-questions/artifact-lifecycle.md`. Its migration is now V4.

**Open product decisions (ask; never assume):**
- Are `QuickMatchGame` and `TapDashGame` keepers or placeholders?
- Spark reveal: option (a) or (b) above?
- The due-for-review algorithm.
- The streak mechanic.
- Should Folder colours be user-picked or auto-assigned from a palette?

## 7. Code layout (`learning-dashboard-release-2026-09-27.zip`)

```
START_HERE.md
docs/                       PROJECT_HANDOFF_V3, MASTER_ROADMAP, ARCHITECTURE_DIAGRAMS, CODEBASE_BASELINE
learning-dashboard-backend/ pom.xml, src/main/java/com/learningdashboard/backend/{common,config,concept,generation/{job,model,pipeline,prompt,provider,validation},rag,ratelimit,security,storage,user,web},
                            src/main/resources/{application.yml,db/migration}, src/test, docs/{design,VERIFY_*}, set-env.example.ps1
learning-dashboard-frontend/ src/{api,app,auth,config,domain,features/{capture,generate,spark,workspace},pages,shared}, scripts/ (Phase 6 product check)
```
**In the user's monorepo these two folders are named `backend/` and `frontend/`** (confirmed 2026-09-27); the zip uses the longer names. There's one canonical copy of each project. The old duplicate folders (`backend/`, `backend_working/`, `*_3`, `*_5`) are gone; if the user's local disk still has them, they're stale.
