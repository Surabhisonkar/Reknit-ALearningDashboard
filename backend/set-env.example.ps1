# Copy to set-env.ps1 (gitignored) and fill in real values. Never commit set-env.ps1.
# ---- Cognito ----
$env:COGNITO_ISSUER_URI = "<COGNITO_ISSUER_URI>"
$env:COGNITO_USER_POOL_ID = "<COGNITO_USER_POOL_ID>"
$env:COGNITO_APP_CLIENT_ID = "<COGNITO_APP_CLIENT_ID>"
$env:COGNITO_REGION = "<COGNITO_REGION>"

# ---- Database ----
$env:DB_URL = "<DB_URL>"
$env:DB_USERNAME = "<DB_USERNAME>"
$env:DB_PASSWORD = "<DB_PASSWORD>"

# ---- Redis (Memurai) ----
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"

# ---- AI providers (Gemini first; paid providers only if Gemini fails) ----
# A provider whose key is not set is dropped from the chain at startup, so
# this order is safe with only GEMINI_API_KEY filled in. To add the paid
# fallbacks, set ANTHROPIC_API_KEY and/or OPENAI_API_KEY.
$env:GEMINI_API_KEY = "<GEMINI_API_KEY>"
# $env:ANTHROPIC_API_KEY = "<ANTHROPIC_API_KEY>"
# $env:OPENAI_API_KEY = "<OPENAI_API_KEY>"
$env:TEXT_PROVIDER_ORDER = "gemini,claude,openai"
$env:VISUAL_PROVIDER_ORDER = "gemini"
$env:EMBEDDING_PROVIDER_ORDER = "gemini"
$env:GEMINI_EMBEDDING_MODEL = "gemini-embedding-001"
# ---- AWS ----
$env:AWS_PROFILE = "<AWS_PROFILE>"
$env:AWS_REGION = "<AWS_REGION>"
$env:GENERATION_QUEUE_URL = "<GENERATION_QUEUE_URL>"
$env:GENERATION_DLQ_URL = "<GENERATION_DLQ_URL>"
$env:ARTIFACTS_BUCKET = "<ARTIFACTS_BUCKET>"

# ---- CORS ----
$env:CORS_ALLOWED_ORIGINS = "<CORS_ALLOWED_ORIGINS>"

Write-Host "Environment variables set for this session." -ForegroundColor Green