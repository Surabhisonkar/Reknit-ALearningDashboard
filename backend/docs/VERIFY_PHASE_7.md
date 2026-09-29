# Verify Phase 7: Folders + Library

Branch: `feature/phase-7-folders`. Do the steps in order; stop and paste the real error if one fails.

## 1. Try `V3__folders.sql` on a copy of your database first

V3 drops the old `concepts.folder` column, and MySQL can't roll back schema changes. A two-minute dry run on a copy is worth it.

```powershell
# PowerShell, from the repo root. Adjust user/db names to your set-env.ps1.
mysqldump -u root -p --databases learning_dashboard --result-file=ld-before-v3.sql
mysql -u root -p -e "CREATE DATABASE ld_v3_test"
mysqldump -u root -p learning_dashboard | mysql -u root -p ld_v3_test
mysql -u root -p --default-character-set=utf8mb4 ld_v3_test < backend\src\main\resources\db\migration\V3__folders.sql
```

Then look at the result:

```sql
USE ld_v3_test;
SELECT name, color FROM folders ORDER BY name;          -- one row per folder name, case variants merged
SELECT COUNT(*) FROM concepts WHERE folder_id IS NULL;  -- = your concepts that had no folder
SHOW COLUMNS FROM concepts LIKE 'folder%';              -- only folder_id is left
DROP DATABASE ld_v3_test;                               -- when you're happy
```

Use `--default-character-set=utf8mb4` on the `mysql` client, or non-Latin folder names are read back garbled (the app itself connects over JDBC with UTF-8, so it isn't affected).

**If V3 ever fails part-way on the real database:** restore `ld-before-v3.sql`, fix the cause, and make sure Flyway won't skip or block the retry: `DELETE FROM flyway_schema_history WHERE version = '3';` (Flyway is a library dependency here, not a Maven plugin, so there's no `mvn flyway:repair`). Then start the app again. The migration's guard stops it *before* dropping the old column if any folder name failed to map, so a failure there loses no data.

## 2. Backend tests

The assistant's sandbox can't reach Maven Central, so this is the real check:

```powershell
cd backend
mvn test
```

New test classes: `FolderServiceTest`, `FolderQueryServiceTest`, `LeastUsedFolderColorAssignerTest`, `FolderDeletionModeTest` (also covers `FolderColor` and name normalization), `ConceptServiceFolderTest`, `ConceptResponseAssemblerTest`. Changed: `ConceptSaveServiceTest`, `ConceptRegenerationServiceTest`, `RetrievalServiceRelatedToConceptTest`.

## 3. Run it

Start the api and the worker as usual. On startup Flyway applies V3, and Hibernate's `validate` checks the new `folders` table and `concepts.folder_id`.

Quick manual pass on `/library`:
- Your existing folders appear as tiles, each with a colour and count; case variants of one name are a single folder.
- `+` creates a folder; a name that exists in any letter case shows an inline error.
- A card's ⋯ → Move to folder: pick a folder, or `+` to create one (with a colour) and move into it.
- Folder ⋯ → Rename or recolour; Delete folder → "Keep the concepts" or "Delete the concepts too".
- Spark's folder picker still lists your folders (only non-empty ones).
- Capture → Save: the concept lands in the AI-suggested folder, created if it didn't exist.

## 4. Real-API end-to-end check (test account only)

`frontend/scripts/verify-phase7-folders.mjs` drives the real Library UI with Playwright and cross-checks each step through the API. It creates 2 concepts through the real Visualize pipeline (some Gemini quota, 1–2 minutes each) plus a few folders, and deletes everything it created at the end, even when a check fails. It refuses to run without `--test-account`.

```powershell
cd frontend
npm install
npx playwright install chromium
npm run dev    # in another terminal, pointed at the same api
# Sign in AS THE TEST ACCOUNT, then in the browser console:
#   JSON.parse(sessionStorage.getItem("auth_session")).accessToken
node scripts/verify-phase7-folders.mjs --test-account --token <access token>
```

## What the assistant verified before delivery

- `V1 → V2 → V3` on a real MySQL 8.0.46 (server default collation `utf8mb4_0900_ai_ci`), seeded with case-only variants, a tie, accents (`Café`/`Cafe`), `Straße`/`Strasse`, Greek case variants, blank and space-padded names, two users sharing a name, and a user with 9 folders. All mapped as designed.
- The guard: with one mapping deliberately broken, V3 stopped before dropping anything.
- Runtime SQL: the unique key rejects a case variant; the find-or-create insert is a silent no-op on a duplicate; deleting a folder unfiles its concepts; deleting a user removes their folders.
- Backend: `javac` parse/analysis; no unresolved references between the project's own classes.
- Frontend: `npm run lint`, `npm run build`, and 29 Playwright checks against a mock that follows the API contract.
