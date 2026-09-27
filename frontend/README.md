# Learning Dashboard Frontend

A React + Vite visual learning workspace. This revision wires the app to
the real Spring Boot backend (Cognito auth, async job-based generation,
server-persisted concepts) — previously most screens were static mockups
with no real backend integration.

## What's real now vs. still a mockup

- **`/create` (Capture)**, **`/workspace`**, **`/library`** — fully wired
  to the real backend: Cognito Hosted UI login required, Explain +
  Visualize as separate async jobs, real persisted concepts, real
  generated visuals via presigned S3 URLs.
- **`/generate`**, **`/spark`** — still static design mockups, untouched,
  not wired to any backend. Deliberately out of scope for this pass.

## Setup

```bash
cp .env.example .env.local   # fill in your Cognito + backend values
npm install
npm run dev
```

See `.env.example` for the full list of required variables. All of them
are build-time public client values (Cognito public app client, no
secret) — never put a real secret in a `VITE_*` variable, since anything
with that prefix ships in the browser bundle.

## Auth

Login is Cognito Hosted UI via the OAuth2 Authorization Code + PKCE flow,
hand-rolled with the Web Crypto API (`src/auth/`) rather than pulling in
AWS Amplify. Session tokens live in `sessionStorage` (cleared when the
tab closes), never `localStorage`. `RequireAuth` wraps any route that
touches real user data and redirects to Hosted UI if not signed in.

## Commands

```bash
npm run dev
npm run build
npm run lint
npm audit
```

All four are also run in CI (`.github/workflows/frontend-ci.yml`) against
this exact toolchain.

## Routes

- `/` landing page
- `/create` concept capture — Explain (optional) then Visualize, both async jobs
- `/library` your saved concepts (server-persisted, not localStorage)
- `/workspace?conceptId=...` a saved concept's visualization
- `/generate`, `/spark` — still design mockups
- `/about` about page
- `/auth/callback` — Cognito Hosted UI redirect target, not a user-facing page

## Capture drafts + concept versions (2026-09-25)

- **Capture** now shows the generated visualization inline as an unsaved **draft** with Save / Regenerate / Discard. Save calls `POST /api/concepts`, and a title clash opens the rename / replace / keep-both modal *before* anything is saved.
- **Concept detail** (`/workspace`) keeps only the title, summary and type badge as chrome. Regenerate and Delete sit behind the ⋯ menu. Version dots (the animation renderer's scene-dot component, `shared/ui/ProgressDots`) appear once a concept has more than one version.
- Verified with `npm run lint` and `npm run build`, plus a headless-browser run of both flows against a mocked API.

## Related concepts (2026-09-26)

Concept detail shows a **"Connects to"** row of saved concepts related to this one, across folders. It renders nothing while loading, on error, or when there are none. The data comes from `GET /api/concepts/{id}/related` via `api/relatedConceptsApi.js`, `domain/relatedConceptMapper.js` and `features/workspace/hooks/useRelatedConcepts.js`.

To check that the connections are sensible, save the concepts in `scripts/related-concepts-fixtures.md`, then run `node scripts/verify-related-concepts.mjs --token <token>`.
