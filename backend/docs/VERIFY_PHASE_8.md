# Verify Phase 8: Spark feed + Ask the AI + Notes

Branch: `feature/phase-8-spark`. Do the steps in order; stop and paste the real error if one fails.

## 1. Backend tests

The assistant's sandbox can't reach Maven Central, so this is the real check:

```powershell
cd backend
mvn test
```

26 test classes. New: `SparkFeedSamplerTest`, `ChatHistoryPolicyTest`, `ConceptChatPromptBuilderTest`, `ChatReplyParserTest`, `ConceptNoteServiceTest`, `ChatJobHandlersTest`. Changed: `ConceptServiceFolderTest` (the service now takes the sampler; one new feed test).

## 2. Run it

`V4__concept_notes.sql` only adds a table (nothing is dropped), so no database dry run is needed this time. Start the api and the worker as usual; Flyway applies V4 and Hibernate validates the new `concept_notes` table.

Optional new settings (defaults shown):

| Env var | Default | What |
|---|---|---|
| `ASK_RATE_MAX` / `ASK_RATE_WINDOW_SECONDS` | 20 / 60 | "Ask the AI" + "Save as note", per user |

The feed mix lives in `application.yml` under `app.spark.mix` (keys are in `[brackets]` on purpose: Spring Boot would otherwise turn `mind_map` into `mindmap`).

Quick manual pass on `/spark`:
- Cards open as a teaser; tap reveals the visual with its entrance and the narrator reads it (mute is remembered).
- Swipe (phone) or wheel / arrow keys / the arrows (desktop); a game appears now and then, never twice in a row.
- On a revealed card: the layers icon opens related concepts (swipe left for more, right to go back).
- Deep dive (sparkle) → book opens Concept Detail; chat bubble opens Ask the AI.
- In the chat: ask twice, then **Save as note** → it appears under **Notes** on Concept Detail (and can be deleted); or **Make a visual** → Capture opens pre-filled.
- Folder mode works; Due for review says "coming soon".

## 3. Real-API end-to-end check (test account only)

`frontend/scripts/verify-phase8-spark.mjs` checks the API contract (feed mix, ask, validation, 404s, notes, the delete cascade) and drives the real Spark UI. It creates 2 concepts through the real Visualize pipeline plus a few AI answers and notes, and deletes everything it created at the end, even when a check fails. It refuses to run without `--test-account`.

```powershell
cd frontend
npm run dev    # in another terminal, pointed at the same api
# Sign in AS THE TEST ACCOUNT, then in the browser console:
#   JSON.parse(sessionStorage.getItem("auth_session")).accessToken
node scripts/verify-phase8-spark.mjs --test-account --token <access token>
```

## What the assistant verified before delivery

- `V1 → V4` on a real MySQL 8.0.46: deleting a concept deletes its notes; deleting a user deletes theirs; a note can't point at a missing concept; column types match the entity.
- Backend: `javac` parse/analysis with no unresolved references between the project's own classes, **and no unresolved JDK classes** (a new check: it would have caught the missing `import java.util.UUID` in Phase 7; proven by re-introducing that bug).
- Frontend: `npm run lint`, `npm run build`, 23 Spark Playwright checks against a mock that enforces the real API limits (including real touch events through Chrome DevTools, and the old touch behaviour proven to fail), and the 29 Library checks as a regression.
