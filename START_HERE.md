# Reknit: A Learning Dashboard — start here

Release **2026-10-06**: adds the visual failsafe, React Flow mind maps and diagrams, coded animations and the Capture type picker. Built on release 2026-09-27.


- **Backend:**
  - Copy `set-env.example.ps1` to `set-env.ps1` and fill it in (never commit it).
  - Run `. .\set-env.ps1`, `mvn test`, and `mvn spring-boot:run`.
  - In a second terminal, set `$env:SPRING_PROFILES_ACTIVE="worker"` and run `mvn spring-boot:run` again.

- **Frontend:**
  - `npm ci` (this release adds `@xyflow/react`)
  - `npm run dev` (it needs `.env.local`; see `.env.example`).
