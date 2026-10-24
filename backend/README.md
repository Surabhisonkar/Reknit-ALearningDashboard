The backend reads all of its configuration from environment variables. There is a PowerShell template to start from:

```powershell
cd backend
copy set-env.example.ps1 set-env.ps1    # fill in your values; this file is gitignored
. .\set-env.ps1
mvn test
mvn spring-boot:run                     # API on port 8080
```

Then start the worker in a second terminal:

```powershell
. .\set-env.ps1
$env:SPRING_PROFILES_ACTIVE = "worker"
mvn spring-boot:run                     # worker on port 8081
```

On macOS or Linux, export the same variables in your shell and run `SPRING_PROFILES_ACTIVE=worker mvn spring-boot:run` for the worker. Flyway creates the schema the first time the API starts.

**Both processes need to be running.** Without the worker, jobs are accepted and then sit in `PENDING`.
