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

# ---- AI providers ----
$env:GEMINI_API_KEY = "<GEMINI_API_KEY>"
$env:TEXT_PROVIDER_ORDER = "gemini"
$env:EMBEDDING_PROVIDER_ORDER = "gemini"
$env:GEMINI_EMBEDDING_MODEL = "gemini-embedding-001"

# Illustrations: Qwen-Image first, Gemini as the fallback. A provider without
# a key drops out of the chain, so this still works with only GEMINI_API_KEY.
$env:VISUAL_PROVIDER_ORDER = "qwen,gemini"
# Alibaba Cloud Model Studio key, created in the international (Singapore) region.
$env:DASHSCOPE_API_KEY = "<DASHSCOPE_API_KEY>"

# Optional: Remotion render service for animation MP4s (see animation-renderer/README.md).
# Leave it unset and animations play live in the browser instead.
$env:ANIMATION_RENDERER_URL = "http://localhost:3100"

# ---- AWS ----
$env:AWS_PROFILE = "<AWS_PROFILE>"
$env:AWS_REGION = "<AWS_REGION>"
$env:GENERATION_QUEUE_URL = "<GENERATION_QUEUE_URL>"
$env:GENERATION_DLQ_URL = "<GENERATION_DLQ_URL>"
$env:ARTIFACTS_BUCKET = "<ARTIFACTS_BUCKET>"

# ---- CORS ----
$env:CORS_ALLOWED_ORIGINS = "<CORS_ALLOWED_ORIGINS>"

Write-Host "Environment variables set for this session." -ForegroundColor Green