# Codebase Baseline & Engineering Standards

First written 2026-09-25, updated 2026-09-27. Read it alongside `PROJECT_HANDOFF_V3.md`. §5 (standards) is binding for all new work.

---

## 1–3. Codebase state

**Updated 2026-09-27.** The original 2026-09-25 findings (duplicate copies, dead files, handoff drift) are all resolved. The canonical code is `learning-dashboard-release-2026-09-27.zip`: one backend and one frontend, 14 unused frontend files removed. For what's built, see `PROJECT_HANDOFF_V3.md` §3; for the design, `ARCHITECTURE_DIAGRAMS.md`.

## 4. Known issues: status

| # | Item | Status |
|---|---|---|
| S1 | Real Gemini key + DB password in `set-env.ps1` shipped in a zip | 🟡 File gitignored, `set-env.example.ps1` added, excluded from zips. **User must rotate the Gemini key.** |
| C1 | Embedding-model default `text-embedding-004` (shut down) | ✅ Fixed in `application.yml` and `AiProperties.java` → `gemini-embedding-001` |
| H1 | `VisualizePipeline` god-class (10 deps, persisted) | ✅ Phase A: persists nothing, 6 deps |
| H2 | `generation` → `concept` repository coupling | 🟡 `generation` fixed in Phase A (ports). **Corrected 2026-09-27:** `rag.ConceptIndexJobHandler` *and* `rag.RetrievalService` still inject `ConceptRepository`. See `TECH_DEBT_TASKS.md` T1 |
| H3 | `switch` on `JobType` | ✅ `JobHandler` registry |
| H4 | `switch` in `VisualPayloadValidator` | ❌ open |
| H5 | `visualizationType` as a raw String; `"animation"` hard-coded | ❌ open |
| H6 | Orchestration + rate limiting inline in controllers | ❌ open |
| H7 | `ConceptController` also serves Spark/folders | 🟡 Phase 7: folder routes moved to `FolderController` / `ConceptFolderController`; Spark's feed is still on `ConceptController` |
| H8 | One `ResponseMapper` for everything | ❌ open (5 methods now) |
| H9 | Frontend renderers fetch artifact URLs via auth+api | ❌ open. Needed before the audio layer (Phase 10) |
| H10 | `accessToken` threaded through every API call | ❌ open |
| H11 | `TYPE_LABELS` duplicated; inline styles | ✅ Phase 7: `LibraryPage` uses the shared constant and has no inline styles |
| T1 | Worker rollback-only trap in `JobProcessingService` | ❌ open, pre-existing. See handoff §4.1 |
| L1 | **Artifact lifecycle not enforced** (parked behind Phase 7; migration now V4; see `docs/open-questions/artifact-lifecycle.md`) (no sweeper, no bucket rule, deleted concepts' and purged users' images leak; GDPR gap) | ❌ **Next up.** Designed: `design/artifact-lifecycle-cleanup.md` |
| H12 | `web.ArtifactController` → `storage.ArtifactRepository` | ❌ `TECH_DEBT_TASKS.md` T2 |
| H13 | `security.UserProvisioningService` → `user.UserRepository` | ❌ `TECH_DEBT_TASKS.md` T3 |
| M1 | `ArtifactStorage.presignUploadUrl` has no caller | ❌ `TECH_DEBT_TASKS.md` T4 |
| M2 | `backend/.github/modernize/` (Copilot leftovers, already git-ignored) | ❌ `TECH_DEBT_TASKS.md` T5 |
| M3 | The V2 migration header comment is outdated (never edit applied migrations) | Accepted; documented |
| M5 | Bad query/path parameters return 500, not 400, API-wide | ❌ `docs/open-questions/api-error-codes.md` |
| M4 | CI workflows sit inside the subprojects, but the monorepo only runs root `.github/workflows` | ❌ Part of the L1 change |

## 5. Engineering standards (binding for all new work)

1. **Design before code.** Any feature that adds or changes classes, modules, or tables starts with a class diagram and/or schema (ER) design, which the user reviews before implementation. Trivial fixes are exempt.
2. **SOLID, applied concretely:**
   - *S*: one reason to change per class. Pipelines orchestrate. They don't persist, validate and index all at once.
   - *O*: new job types, payload types, providers and games are added by writing a new class and registering it, never by editing a `switch`.
   - *L*: every implementation of a port is fully substitutable. Tests run against the interface.
   - *I*: small, role-specific interfaces (e.g. `ConceptReader` versus `ConceptWriter`, not one fat service).
   - *D*: modules depend on interfaces they own ("ports"). Concrete adapters (S3, SQS, Gemini, MySQL repos) are wired only in config.
3. **Low coupling between modules.** A module never injects another module's repository or entity internals. It talks through a published interface or a domain event. Integrating class A into B means depending on an interface of A, so B can be tested with a fake A.
4. **Registries over conditionals** for anything type-keyed, on both backend and frontend.
5. **Frontend layering:** `pages` compose `features`. `features` use `domain` + `api` through hooks. `shared/ui` stays presentational and never imports `api` or `auth`.
6. **Verification:** frontend is lint + build clean before delivery (plus Playwright against a mocked API where UI flows change). Backend can't be compiled in the sandbox (Maven Central returns 403, still true as of 2026-09-27), so say so every time, and the user's `mvn test` output is the source of truth. Do run the `javac` parse/analysis check. Validate that every Mermaid diagram renders.
7. The existing working agreements (Handoff Part 9) still apply: UI locked page by page before backend wiring, and no infra/scaling work until features are built.
8. **Confirm before code (user rule, 2026-09-26).** Before generating any code, raise open questions and confirm the plan with the user: scope, any deviation from the roadmap or diagrams, and design choices with more than one reasonable answer. Implement only after explicit agreement. A deviation that seems clearly better still gets asked, never just done and explained afterwards.
9. **Canonical design documents.** `MASTER_ROADMAP.md` (phase order and status) and `ARCHITECTURE_DIAGRAMS.md` (class diagrams, ER, use cases) are the user's source of truth. New work follows them. When code and a diagram disagree, raise it; don't silently pick one. After a phase ships, the affected diagram sections are regenerated from the real source.

## 6. Current status

Phases A+B (draft/confirm-save, versioning) and Phase 6 / F (related concepts, steps 1–4) are delivered. See `MASTER_ROADMAP.md` for what's next, and `claude/design/` for per-phase designs.
