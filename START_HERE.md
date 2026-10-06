# Learning Dashboard — start here

Release **2026-10-06**: adds the visual failsafe, React Flow mind maps and diagrams, coded animations and the Capture type picker (see `backend/docs/design/visual-failsafe-and-provider-routing.md` and `docs/VERIFY_FAILSAFE.md` beside it). Built on release 2026-09-27.

1. Read `docs/PROJECT_HANDOFF_V3.md` first: the product, the rules, what's built, and what's next.
2. `docs/MASTER_ROADMAP.md`: phase order and status.
3. `docs/ARCHITECTURE_DIAGRAMS.md`: class and ER diagrams (all 16 render).
4. `docs/CODEBASE_BASELINE.md`: binding engineering standards and the tech-debt list.
5. Per-phase designs and verify guides: `backend/docs/`.

**Run it**

- **Backend:** copy `set-env.example.ps1` to `set-env.ps1` and fill it in (never commit it). Then run `. .\set-env.ps1`, `mvn test`, and `mvn spring-boot:run`. In a second terminal, set `$env:SPRING_PROFILES_ACTIVE="worker"` and run `mvn spring-boot:run` again.
- **Frontend:** `npm ci` (this release adds `@xyflow/react`), then `npm run dev` (it needs `.env.local`; see `.env.example`).
