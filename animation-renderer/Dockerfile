# Build from the repository root, because the composition lives in packages/storyboard/:
#   docker build -f animation-renderer/Dockerfile -t reknit-animation-renderer .
FROM node:22-bookworm-slim

# Shared libraries Chrome Headless Shell needs (see remotion.dev/docs/docker).
RUN apt-get update && apt-get install -y --no-install-recommends \
      libnss3 libdbus-1-3 libatk1.0-0 libgbm-dev libasound2 libxrandr2 libxkbcommon-dev \
      libxfixes3 libxcomposite1 libxdamage1 libatk-bridge2.0-0 libpango-1.0-0 libcairo2 libcups2 \
      fonts-noto-color-emoji \
    && rm -rf /var/lib/apt/lists/*

# The shared composition package, linked from package.json as file:../packages/storyboard.
COPY packages/storyboard/ /app/packages/storyboard/

WORKDIR /app/animation-renderer
COPY animation-renderer/package.json animation-renderer/package-lock.json ./
RUN npm ci --omit=dev && npx remotion browser ensure

COPY animation-renderer/ ./

RUN useradd --create-home renderer && chown -R renderer /app
USER renderer
EXPOSE 3100
CMD ["node", "server.mjs"]
