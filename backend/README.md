# Learning Dashboard Backend — Phase 1 + 1b, MySQL edition (validated by a real run)

This is **not a finished product** — it's the foundation phase: DB, auth,
async job orchestration, provider-neutral AI adapters with fallback, the
rich visualization schema, and server-side RAG.

**Status**: this revision has been compiled and run against a real MySQL
8.0 instance (not just reviewed by reading). Flyway validated the schema,
Hibernate/JPA initialized cleanly against it, and the app got as far as
starting Tomcat before hitting one real bug — a `@Transactional` Servlet
`Filter` getting CGLIB-proxied, which broke its container-managed
lifecycle. That bug (and a handful of other real issues a compiler/runtime
caught) are fixed in this revision — see "Fixes applied in this revision"
below. This is meaningfully more confidence than any earlier revision of
this backend has had.

## What changed in the MySQL migration

- `V1__init_schema.sql` rewritten for MySQL: `InnoDB` tables, `CHAR(36)`
  UUID columns (always app-assigned via `UUID.randomUUID()`, never a DB
  default — no MySQL equivalent of `pgcrypto` needed), native `JSON`
  columns in place of `jsonb`.
- Every entity's `UUID` fields now carry `@JdbcTypeCode(SqlTypes.CHAR)` so
  Hibernate binds/reads them as that exact `CHAR(36)` representation,
  rather than relying on whatever Hibernate's MySQL dialect defaults to for
  `java.util.UUID` (that default isn't something I could verify without a
  compiler in the loop, so I made it explicit instead of guessing).
- **No native vector type in MySQL** (unlike Postgres+pgvector), so RAG
  retrieval changed shape: `ConceptEmbedding.embedding` is now a `float[]`
  stored as a JSON array via a hand-rolled `VectorJsonConverter` (a plain
  `AttributeConverter`, not a Jackson dependency — JPA converters aren't
  Spring-managed beans, so keeping it dependency-free avoids extra wiring).
  `RetrievalService` now loads a user's embeddings and ranks them by
  in-app cosine distance (`CosineSimilarity`) instead of a pgvector `<=>`
  query. This is a **documented, deliberate scaling trade-off** — fine at
  the per-user concept counts a personal learning app expects; if that
  changes, `RetrievalService.findRelated(...)` is the entire contract a
  future OpenSearch-backed implementation would need to satisfy, with zero
  caller changes anywhere else.
- `org.postgresql:postgresql` → `com.mysql:mysql-connector-j`;
  `flyway-database-postgresql` → `flyway-mysql`; Testcontainers'
  `postgresql` module → `mysql` module.
- **Removed** `com.pgvector:pgvector` and `PgVectorType` entirely — no
  longer needed.
- **Bucket4j dependency fixed**: `bucket4j_jdk17-core` (my earlier guess,
  which you correctly flagged as wrong) → `bucket4j-core`, the real
  artifact coordinate.

## Fixes applied in this revision (found by an actual compile + run)

1. **Filter/Transactional crash (the one that actually broke startup)**:
   `CognitoUserProvisioningFilter.doFilterInternal` was annotated
   `@Transactional`. Spring AOP-proxies any bean with a `@Transactional`
   method via CGLIB, which (for a bean that's also a Servlet `Filter`,
   instantiated via Objenesis without running field initializers) left
   `GenericFilterBean`'s `logger` field null, and Tomcat's own filter
   lifecycle call to the inherited **final** `init(FilterConfig)` method
   NPE'd on it — `Cannot invoke "Log.isDebugEnabled()" because "this.logger"
   is null`, which crashed the whole application context on startup. Fix:
   moved the transactional DB lookup/insert into a new `UserProvisioningService`
   (a plain `@Service`, safe to proxy), and the filter itself now carries no
   `@Transactional` at all. **General rule going forward: never put
   `@Transactional` directly on a class whose lifecycle is driven by a
   container-called interface with final methods (Servlet `Filter`,
   `Listener`, etc.) — always delegate to a plain service.**
2. **`ClientHttpRequestFactories`/`ClientHttpRequestFactorySettings`
   moved packages** in this Spring Boot version: from
   `org.springframework.http.client.*` to
   `org.springframework.boot.http.client.*`, with a slightly different
   builder API (`ClientHttpRequestFactoryBuilder.detect().build(settings)`
   instead of `ClientHttpRequestFactories.get(settings)`, and
   `ClientHttpRequestFactorySettings.defaults()` instead of a `.DEFAULTS`
   constant). Fixed in all five provider adapters (Claude, 3×Gemini,
   OpenAI). **Any future provider adapter must use the new
   `org.springframework.boot.http.client` API, not the old one.**
3. **Missing `resilience4j-all` dependency**: `ResilientProviderExecutor`
   uses `io.github.resilience4j.decorators.Decorators`, which lives in
   `resilience4j-all`, not `resilience4j-spring-boot3` alone. Added as an
   explicit dependency.
4. **`@Lazy` needed on injection points, not just on `@Bean` methods**:
   marking `claudeTextProvider()` etc. `@Lazy` in `ProviderFactory` only
   defers *that* bean's own construction — it does **not** stop an eagerly-
   created consumer (`textGenerationProvider(...)`, itself an eager
   `@Primary` bean) from being forced to resolve it immediately as a
   required constructor parameter. Without `@Lazy` on the parameter too,
   a missing `OPENAI_API_KEY` would have crashed startup by trying to
   construct `OpenAiTextProvider` even when `openai` isn't in
   `TEXT_PROVIDER_ORDER` at all — defeating the documented "app starts,
   individual providers fail gracefully" behavior. Fixed by adding
   `@Lazy` to the provider parameters in `ProviderFactory`, and the same
   pattern applied to `ProxyManager<byte[]>` in `RateLimiterFactory` plus
   marking `bucketProxyManager()` itself `@Lazy` in
   `RedisProxyManagerConfig` (so the app doesn't try to connect to Redis
   at startup either). **Any future optional/deferred bean needs `@Lazy`
   on both the `@Bean` method and every injection point that consumes it,
   or the deferral does nothing.**

## What's actually built

### Persistence & auth
- Flyway-owned Postgres schema (`V1__init_schema.sql`): `users`, `concepts`,
  `generation_jobs`, `artifacts`, `concept_embeddings` (pgvector)
- Cognito JWT resource server (`SecurityConfig`) + JIT user provisioning
  (`CognitoUserProvisioningFilter`) — every authenticated request gets a
  local `User` row keyed by Cognito `sub`, and every other table's ownership
  check is a foreign key to that local id
- CORS is an explicit allow-list from `CORS_ALLOWED_ORIGINS`, never `*`
- Ownership is enforced in the query itself (`findByIdAndUserId`), and a
  cross-tenant request gets the same 404 as a genuinely missing id

### Async job orchestration
- `GenerationJob` entity/repo — every LLM-touching request creates one of
  these (`PENDING`), returns `202 Accepted`
- `JobQueue` interface + `SqsJobQueue` — real AWS SDK v2 SQS, long-polling
- `GenerationJobWorker` — `@Profile("worker")` only; the api service's
  Spring context never loads this, so a stuck LLM call architecturally
  cannot block an HTTP thread on the public-facing service
- `JobProcessingService` — the one place that actually invokes a pipeline;
  idempotent against SQS's at-least-once delivery (re-checks job status
  before processing)
- `GET /api/jobs/{id}` — poll for status/result

### Provider-neutral AI layer
- `TextGenerationProvider` / `VisualGenerationProvider` / `EmbeddingProvider`
  interfaces; `ClaudeTextProvider`, `GeminiTextProvider`, `GeminiVisualProvider`,
  `GeminiEmbeddingProvider`, `OpenAiTextProvider`
- `ResilientProviderExecutor` — Resilience4j retry + **circuit breaker** per
  provider (a sustained Claude outage now trips its breaker and fails fast
  instead of retry-looping every request)
- `ProviderFallbackExecutor` + three `Fallback*Provider` composites — real
  multi-provider fallback chains, ordered by config
  (`TEXT_PROVIDER_ORDER=claude,gemini,openai`), not just a single selected
  provider
- To add a provider: one new adapter class + one `@Bean` in `ProviderFactory`
  + one config value. Nothing else changes.

### Two separate pipelines, never one call doing both
- `ExplainPipeline` — topic (+ optional user notes) → explanation. One
  `TextGenerationProvider` call.
- `VisualizePipeline` — concept text → server-side RAG retrieval →
  structuring call → `VisualPayloadValidator` → typed payload → per-scene/
  asset `VisualGenerationProvider` calls where the model asked for one →
  two-phase concept save → embedding index. Several distinct calls, each to
  whichever provider is actually good at that step.

### Rich visualization schema (your explicit spec)
- `VisualizationPayload` sealed interface + `MindMapPayload` (nodes, edges,
  layout hints, citations), `DiagramPayload` (typed elements, connections,
  styles, coordinates, accessibility labels), `AnimationPayload` (scenes,
  timing, transitions, asset refs, narration — **scene count and pacing are
  decided by the model per topic**, not hardcoded), `ImagePayload`
- `VisualPayloadValidator` enforces this before anything is persisted or
  returned — the frontend is meant to map this stable shape into renderer
  components, never consume raw LLM JSON

### Server-side RAG
- `ConceptEmbedding` (pgvector) + `RetrievalService` (real `<=>` cosine
  query via `JdbcTemplate`, scoped by `user_id`) + `EmbeddingIndexService`
  — this entirely replaces the old browser-`localStorage` +
  `cosineSimilarity.js` approach. Nothing RAG-related runs client-side now.

### Artifacts
- `Artifact` entity + `ArtifactStorage`/`S3ArtifactStorage` — real S3
  upload, presigned GET/PUT (`S3Presigner`), orphan-expiry lifecycle
  (24h TTL cleared once an artifact is attached to a saved concept)

### Distributed rate limiting
- `DistributedRateLimiter` (Bucket4j + Redis/Lettuce) keyed by
  **authenticated user id**, not IP — shared across every ECS task, unlike
  the earlier in-memory-per-task version

### Error handling
- `GlobalExceptionHandler` — stable, generic client-facing messages only;
  stack traces and provider internals never leave the server log

## Phase A + B: draft → confirm-save, concept versioning (2026-09-25)

Design (class diagram, schema, sequences): `docs/design/phase-a-b-draft-save-and-versioning.md`. How to verify: `docs/VERIFY_PHASE_A_B.md`.

- `POST /api/jobs/visualize` **without** `conceptId` now produces a **draft** (`resultPayload.kind = "DRAFT"`). Nothing is written to `concepts`. Generated images upload as orphans (24h expiry) until saved.
- `POST /api/concepts` (new, confirm-save) takes `{ jobId, title?, folder?, onDuplicate? }`. It checks for a duplicate title **before** writing (409 `DUPLICATE_TITLE`), creates the concept and version 1, attaches the artifacts, and queues an `INDEX_CONCEPT` embedding job after commit. It is idempotent per draft.
- `POST /api/jobs/visualize` **with** `conceptId` = post-save regenerate. It appends version N+1 (`kind = "VERSION"`), and `conceptText` becomes optional because the server reuses the concept's original input.
- `GET /api/concepts/{id}/versions`, `GET /api/concepts/{id}/versions/{n}`: version history (these already existed).
- Internals: `JobProcessingService` dispatches to one `JobHandler` per `JobType` (add a job type = add a handler bean). `VisualizePipeline` no longer persists anything. The `concept` module talks to generation/storage/rag only through its own ports (`ConceptDraftSource`, `ConceptArtifactLinker`, `ConceptIndexer`).
- `GEMINI_EMBEDDING_MODEL` now defaults to `gemini-embedding-001` (`text-embedding-004` was shut down).
- `set-env.ps1` is gitignored. Copy `set-env.example.ps1` to create yours.

## Phase F: related concepts, RAG as a user-facing feature (2026-09-26)

Design: `docs/design/phase-f-related-concepts.md`. How to verify: `docs/VERIFY_PHASE_F.md`.

- `GET /api/concepts/{id}/related` → `[ { id, title, summary, distance } ]`, nearest first. It is ownership-checked (404 otherwise), excludes the concept itself, and ignores folders on purpose.
- It uses the concept's **stored** embedding as the query, so a page view never calls Gemini (the api process stays provider-free). A concept that isn't indexed yet returns `[]`.
- Results are capped by `RELATED_TOP_K` (5) and filtered by `RELATED_MAX_DISTANCE` (0.35, cosine distance). Calibrate the threshold with the Step 5 script.

## Known gaps and unverified areas — read this before you run it

**Good news first**: the run log you sent shows `mvn spring-boot:run`
successfully compiling the entire project ("Nothing to compile - all
classes are up to date" on both `compile` and `testCompile`), connecting
to a real MySQL 8.0 instance, Flyway validating the schema, and Hibernate/
JPA initializing the `EntityManagerFactory` cleanly against it — meaning
**`ddl-auto: validate` passed**, which directly confirms the `CHAR(36)`
UUID mapping and the `json` column definitions are correct. That resolves
what were previously my top two flagged risk areas. Remaining risk, ranked:

1. **No request has actually been served yet** — the app crashed during
   filter startup (now fixed) before any controller, pipeline, or the
   worker's SQS loop ever ran. So while the schema/entity mapping is
   confirmed, none of the following have been exercised end-to-end yet:
   `VectorJsonConverter`'s actual read/write round-trip through a live
   query, Jackson's polymorphic deserialization of `VisualizationPayload`
   from real JSON, the Cognito JWT validation path with a real token, the
   SQS producer/consumer round-trip, and `DistributedRateLimiter` against
   a live Redis (the `bucketProxyManager` bean is now `@Lazy`, so a Redis
   connection has likely never actually been attempted yet either). This
   is the natural next thing to verify — get the app started (should work
   now) and hit `POST /api/jobs/explain` for real.
2. **bucket4j-redis API surface** — compiles cleanly now (confirmed by
   the log), which resolves my biggest earlier worry about whether the
   classes/methods even exist. What's still unverified is *runtime*
   behavior against a live Redis instance (see #1).
3. **Nothing here has been run against a real Cognito User Pool, real SQS
   queue, or real S3 bucket** — the MySQL/Redis-adjacent parts are ahead
   of these in verification now.

## Phase 1b additions in this revision

- **GDPR data deletion**: `DataDeletionService` (request/cancel/purge),
  `AccountPurgeJob` (scheduled hard-delete past a grace period, opt-in via
  `GDPR_PURGE_ENABLED` so it doesn't double-run across every ECS task by
  default), `AccountController` (`GET/DELETE /api/account`,
  `POST /api/account/cancel-deletion`). The hard purge is a single `DELETE`
  on the `users` row — every other table cascades via the FK constraints
  already in the schema. S3 objects are reclaimed by a bucket lifecycle
  policy, not by this code path (kept the purge DB-transactional rather
  than also looping S3 deletes that could partially fail).
- **Content-safety validation on AI output**: `ContentSafetyValidator`,
  wired into both `ExplainPipeline` (title/explanation) and
  `VisualizePipeline` (title/summary) right after each provider's text is
  parsed, before anything is persisted. Checks control characters and
  HTML/script injection unconditionally, plus an operator-extensible
  disallowed-terms list (`CONTENT_SAFETY_DISALLOWED_TERMS`, empty by
  default) — this isn't a moderation ML model, it's a cheap backstop most
  useful once any feature makes one user's content visible to another.
- **Pure-logic unit tests** (no Spring context, DB, or Redis required —
  deliberately scoped this way so they're genuinely low-risk to trust
  without a compiler in the loop): `VisualPayloadValidatorTest` (one case
  per visualization type plus key rejection paths),
  `ContentSafetyValidatorTest`, `ProviderFallbackExecutorTest`,
  `CosineSimilarityTest`, `VectorJsonConverterTest`,
  `ExplainPromptBuilderTest`, `VisualizationPromptBuilderTest`.

## What's explicitly NOT in this phase (deferred to Phase 1c)

Terraform/IaC, CI/CD pipeline, frontend boundary refactor (API client +
domain mappers), logging-sanitizer pass, and integration/Spring-context
tests. These are real, sizable pieces of work; I deferred them rather than
rushing them in the same pass as a database migration, on the theory that
correctness matters more than breadth this round.

## Continuous integration

`.github/workflows/backend-ci.yml` runs on every push/PR touching
`backend/`:
- Compiles and runs the full test suite against **real MySQL 8.0 and
  Redis 7 service containers** (not mocks) — a green run is a stronger
  signal than my own review ever could be, for exactly the reason your
  `run-log.txt` was so useful this round.
- No real AI provider keys are used or needed — tests must mock the
  provider interfaces, never call a real adapter with a real key from CI.
- A separate `dependency-check` job runs OWASP Dependency-Check against
  the resolved dependency tree and fails on any HIGH-severity (CVSS ≥ 7)
  known CVE, uploading the HTML report as a build artifact. This is item
  17 from the security checklist (backend half — the frontend half of this
  is already covered too: `frontend/.github/workflows/frontend-ci.yml` has
  had its own `npm audit --audit-level=low` step for a while now. This
  note used to say that workflow still needed to be written; it didn't -
  verified 2026-09-21 by reading the actual file).

This does not need an AWS account to be useful and doesn't deploy
anything yet — that's what the Terraform/IaC work (next in Phase 1c) will
plug into, likely as a second workflow gated on this one passing.

## Setting this up to actually run it

1. **Cognito**: create a User Pool + App Client (no client secret, since
   this is a public SPA + bearer-token API pattern). You'll get a pool id
   and issuer URL like
   `https://cognito-idp.<region>.amazonaws.com/<pool-id>`.
2. **MySQL**: any MySQL 8.0+ (RDS MySQL, or local Docker `mysql:8` for
   dev). No extensions needed. Flyway creates the schema on startup.
3. **Redis**: local Docker `redis:7` for dev, ElastiCache in production.
4. **SQS**: create a standard queue + a DLQ, set a redrive policy
   (maxReceiveCount ~5) pointing the main queue at the DLQ — that's a queue
   setting, not application code.
5. **S3**: create the artifacts bucket, no public access.

```bash
export COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXXXX
export DB_URL="jdbc:mysql://localhost:3306/learning_dashboard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=learning_dashboard
export DB_PASSWORD=...
export REDIS_HOST=localhost
export GENERATION_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/.../generation-queue
export ARTIFACTS_BUCKET=your-bucket-name
export ANTHROPIC_API_KEY=sk-ant-...
export GEMINI_API_KEY=...
export OPENAI_API_KEY=sk-...
export CORS_ALLOWED_ORIGINS=http://localhost:5173

mvn clean compile   # DO THIS FIRST - see "Known gaps" above
mvn spring-boot:run                                    # api service (port 8080)
SPRING_PROFILES_ACTIVE=worker mvn spring-boot:run       # worker service (port 8081)
```

## Full env var reference

| Variable | Purpose |
|---|---|
| `COGNITO_ISSUER_URI` | Cognito User Pool issuer (JWT validation) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origin(s), never `*` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres connection |
| `REDIS_HOST` / `REDIS_PORT` | Distributed rate limiter backing store |
| `ANTHROPIC_API_KEY` / `GEMINI_API_KEY` / `OPENAI_API_KEY` | Provider keys |
| `TEXT_PROVIDER_ORDER` | default `claude,gemini` (add `,openai` to include it) |
| `VISUAL_PROVIDER_ORDER` / `EMBEDDING_PROVIDER_ORDER` | default `gemini` |
| `AI_REQUEST_TIMEOUT_MS` / `AI_MAX_RETRIES` | default `20000` / `2` |
| `AWS_REGION` | default `us-east-1` |
| `GENERATION_QUEUE_URL` / `GENERATION_DLQ_URL` | SQS queue URLs |
| `ARTIFACTS_BUCKET` | S3 bucket for generated visuals |
| `EXPLAIN_RATE_MAX` / `VISUALIZE_RATE_MAX` | per-user requests/window, default `10` |
| `WORKER_POLL_ENABLED` | default `true`; set `false` to disable polling for debugging |

## Master checklist status

**Phase 1 (P0) — MySQL edition, pending your compile/test pass:**
DB schema, Cognito auth, provider abstraction + fallback + circuit breaker,
async SQS job orchestration, rich visualization schema + validator, server-
side RAG (in-app cosine similarity over MySQL JSON columns), S3 artifact
storage + lifecycle, distributed rate limiting.

**Phase 1b — this revision:** GDPR data deletion, content-safety
validation on AI output, pure-logic unit tests.

**Phase 1c — in progress:**
- ✅ CI/CD (backend build+test against real MySQL/Redis service containers,
  OWASP dependency-check) — this revision
- ✅ Frontend boundary refactor (API client + domain mappers + thin pages)
  and frontend `npm audit` CI — both listed here as not-yet-done for a
  while, but verified complete 2026-09-21 by reading the actual frontend
  source. Don't redo this work; see `project-status.md` in the project
  for the full verified-status writeup.
- ❌ Terraform/IaC, logging-sanitizer pass, Spring-context/integration
  tests, Privacy Policy/ToS, keep-Node-live progressive routing
