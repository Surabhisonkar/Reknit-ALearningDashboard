# Reknit: Learning Dashboard — start here

**Run it**

- **Backend:** copy `set-env.example.ps1` to `set-env.ps1` and fill it in (never commit it). Then run `. .\set-env.ps1`, `mvn test`, and `mvn spring-boot:run`. In a second terminal, set `$env:SPRING_PROFILES_ACTIVE="worker"` and run `mvn spring-boot:run` again.
- **Frontend:** `npm ci`, then `npm run dev` (it needs `.env.local`; see `.env.example`).
