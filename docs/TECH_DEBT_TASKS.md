# Tech-debt tasks — ready for a future chat

Written 2026-09-27. Each task is self-contained: the problem, why it matters, exact steps, and how to verify. **The user deferred these on purpose. Don't start one without confirming it first** (standing rule: confirm before code; for any new or changed class, show a Mermaid class diagram first). File paths are relative to `backend/src/main/java/com/learningdashboard/backend/` unless stated.

Suggested order: T1 → T2 → T3 (T1 also clears most of the related H2 remainder), then T4 and T5 whenever convenient.

---

## T1. `rag` reads `concept`'s repository directly (two classes)

**Problem.** `rag/ConceptIndexJobHandler` injects `concept.ConceptRepository` (`findByIdAndUserId`, line ~48). `rag/RetrievalService` also injects it (`findAllById`, lines ~59 and ~124). That breaks the coupling rule in `CODEBASE_BASELINE.md` §5.3. `CODEBASE_BASELINE` H2 wrongly said only `RetrievalService` still does this.

**Fix: a published read port owned by `concept`.**

1. Create `concept/ConceptSnapshot.java`: `record ConceptSnapshot(UUID id, UUID userId, String title, String summary)`. It is read-only and deliberately not the JPA entity, so `rag` can't mutate concepts.
2. Create `concept/ConceptLookup.java` (interface):
   - `Optional<ConceptSnapshot> findOwned(UUID conceptId, UUID userId)`
   - `List<ConceptSnapshot> findAllByIds(Collection<UUID> ids)`
3. Implement it in a new `concept/JpaConceptLookup.java` (`@Component`, wraps `ConceptRepository`, maps entity → snapshot). Don't put it on `ConceptService`, which is already large (ISP).
4. `rag/ConceptIndexJobHandler`: replace the `ConceptRepository` constructor parameter with `ConceptLookup`. Use `findOwned(conceptId, job.getUserId())`, then `snapshot.title()` / `summary()`. The behaviour is unchanged (a missing concept → `{"indexed":false,...}`).
5. `rag/RetrievalService`: replace `ConceptRepository` with `ConceptLookup`. In both places, `conceptRepository.findAllById(ids)` becomes `conceptLookup.findAllByIds(ids)`, building the `Map<UUID, ConceptSnapshot>` the same way. Leave `findRelated` and `findRelatedToConcept` otherwise unchanged.
6. Tests:
   - `RetrievalServiceRelatedToConceptTest` currently mocks `ConceptRepository.findAllById(anyIterable())`. Switch it to mock `ConceptLookup.findAllByIds(anyCollection())`, returning snapshots. The assertions stay the same.
   - Add `ConceptIndexJobHandlerTest` (found → indexes with title/summary; missing → `indexed:false`, and `EmbeddingIndexService` is never called).
7. Diagrams: update 1.4 (new port and record), 1.10 (edges now go to `ConceptLookup`). Mark H2 ✅ in `CODEBASE_BASELINE.md`.

**Verify:** `grep -rn "import com.learningdashboard.backend.concept.ConceptRepository" src/main/java | grep -v /concept/` returns nothing. Then `mvn test`.

---

## T2. `web.ArtifactController` reads `storage.ArtifactRepository` directly

**Problem.** The controller does the ownership lookup itself (`artifactRepository.findByIdAndUserId`) and computes `expiresAt` from `AwsProperties`. That puts storage logic in the web layer (thin-controller rule) and couples web to storage internals.

**Fix: move the "owned download link" use case into `storage`.**

1. Create `storage/ArtifactDownloadLink.java`: `record ArtifactDownloadLink(URL url, Instant expiresAt)`.
2. Create `storage/ArtifactAccessService.java` (`@Service`):
   - Dependencies: `ArtifactRepository`, `ArtifactStorage`, `AwsProperties`.
   - Method `ArtifactDownloadLink presignOwnedDownload(UUID artifactId, UUID userId)`.
   - Body: `findByIdAndUserId` → orElseThrow `NotFoundException` (same 404 as today) → `artifactStorage.presignDownloadUrl(artifact)` → `expiresAt = now + presignTtlMinutes`. Move that logic out of the controller verbatim.
3. `web/controller/ArtifactController`: constructor becomes `(ArtifactAccessService, CurrentUserService)`. The method becomes `var link = access.presignOwnedDownload(id, user.getId()); return new ArtifactDownloadResponse(link.url().toString(), link.expiresAt());`. The response shape is unchanged.
4. Test: `ArtifactAccessServiceTest` (owned → URL and expiry; foreign or missing → `NotFoundException`).
5. Diagrams: 1.11 (new service) and 1.13 (`ArtifactController` edges).

**Verify:**
- `ArtifactController` imports nothing from `storage` except `ArtifactAccessService` and `ArtifactDownloadLink`.
- The frontend image/animation renderers still load images (the Playwright suite, or a manual check).
- `mvn test`.

---

## T3. `security.UserProvisioningService` uses `user.UserRepository`

**Problem.** User lifecycle logic (find-or-create from a JWT, profile updates) lives in `security` but owns `user`'s repository. `AccountController` (profile update) and `CognitoUserProvisioningFilter` both call it.

**Fix: move the user-lifecycle service into `user`, and keep `security` thin.**

1. Create `user/UserAccountService.java` (`@Service`) with:
   - `@Transactional User findOrCreateByCognitoIdentity(String cognitoSub, String emailOrNull, String displayNameOrNull)`. Same logic as today: `findByCognitoSub`, otherwise create `new User(sub, email != null ? email : "unknown", displayName)`. Keep the `"unknown"` fallback.
   - `@Transactional User updateProfile(UUID userId, String email, String displayName)`. Move it verbatim, **including its Javadoc** explaining that Cognito *access* tokens don't reliably carry `email`/`name` (handoff lesson #5), so profile data is synced from the frontend's decoded ID token.

   **It takes plain values, not a `Jwt`**, so `user` has no Spring Security dependency.
2. Reduce `security/UserProvisioningService` to a thin adapter. `findOrCreateUser(Jwt jwt)` reads `jwt.getSubject()`, `jwt.getClaimAsString("email")` and `jwt.getClaimAsString("name")` (as `provisionNewUser` does today) and delegates to `UserAccountService`. Keep the name and signature so `CognitoUserProvisioningFilter` doesn't change. **Neither the filter nor this adapter becomes `@Transactional`** (handoff lesson #1: CGLIB/Objenesis NPE on a Servlet filter). The transaction stays on `UserAccountService`.
3. `web/controller/AccountController`: depend on `UserAccountService.updateProfile` instead of `UserProvisioningService`.
4. Tests: `UserAccountServiceTest` (existing user returned; new user created; profile update). The filter behaviour is unchanged.
5. Diagrams: 1.3 (`UserAccountService` in `user`; the `security` → `user` edge now points at a service, not the repository), 1.13 (the `AccountController` edge).

**Verify:**
- `grep -rn "UserRepository" src/main/java | grep -v "/user/"` returns nothing.
- Log in on a fresh DB (just-in-time provisioning still creates the user), and the account profile update works.
- `mvn test`.

---

## T4. `ArtifactStorage.presignUploadUrl` has no caller

**Problem.** It is dead code. It was presumably meant for a "raw learning-material upload" feature that isn't in `MASTER_ROADMAP.md`.

**Steps (only after the user picks one option):**
- **Remove:**
  1. Delete the method from `storage/ArtifactStorage.java`.
  2. Delete its implementation from `storage/S3ArtifactStorage.java`, along with any imports that are then unused (`PresignedPutObjectRequest`, `PutObjectPresignRequest`).
  3. Remove the `uploads/{userId}/` mention from `docs/design/artifact-lifecycle-cleanup.md` only if the erasure code no longer needs it. **Keep erasing `uploads/` in `S3UserFileEraser` anyway**, in case objects were ever written there.
  4. Update diagram 1.11.
- **Keep:** add a Javadoc line saying it's reserved for the future raw-material upload feature and intentionally unused, and list it in the debt table as "accepted".

**Verify:** `grep -rn presignUploadUrl src` shows the expected result, then `mvn test`.

---

## T5. `backend/.github/modernize/java-upgrade/` (Copilot tool leftovers)

**Problem.** It holds `hooks/scripts/recordToolUse.{sh,ps1}` from a GitHub Copilot "Java upgrade" tool, and nothing references it. Its own `.gitignore` is `**/*`, so **git already ignores everything inside it**. It only exists in local copies and zips.

**Steps:**
1. Delete `backend/.github/modernize/` from the working tree.
2. Once the monorepo CI move is done, `backend/.github/` should be empty; delete it too (root `.github/workflows/` is the only one that runs).
3. Nothing else references it (checked with `grep -rn modernize`).

**Verify:** the zip or working tree has no `modernize` folder, and root CI still triggers on a push to `backend/**`.

---

## Notes that need no action (recorded so no one "fixes" them)

- **The V2 migration's header comment is outdated.** It says "See VisualizePipeline for the two persistence strategies"; that logic now lives in `VisualizeJobHandler` / `ConceptVersionWriter`. **Never edit an applied Flyway migration**, because the checksum changes and startup fails validation. The correction lives in the docs only.
- **CI location:** in the monorepo (`backend/`, `frontend/`), GitHub only runs workflows under the repo-root `.github/workflows/`. Moving them there is part of the artifact-lifecycle change, not this list.
