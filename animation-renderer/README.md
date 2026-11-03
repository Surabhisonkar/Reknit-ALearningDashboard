# Animation renderer

A small Node service that turns an animation storyboard into an MP4 with [Remotion](https://www.remotion.dev/).

The backend worker calls it after the LLM has written and the validator has checked a storyboard (4-8 scenes, each with text, visual elements, timing and a transition). The worker stores the MP4 it gets back in S3 as an artifact and puts its id on the payload as `videoArtifactId`. The React app plays that MP4. When there is no MP4 (no renderer configured, demo mode, or a render failed), the app plays the same composition live with `@remotion/player`, so nothing is lost.

The composition itself lives in [`packages/storyboard`](../packages/storyboard/README.md) (`@reknit/storyboard`), which the frontend uses too, so the video and the in-app preview match exactly.

## API

```
POST /render
{ "storyboard": <animation payload as the backend stores it>, "assetUrls": { "<artifactId>": "<presigned URL>" } }
-> 200 video/mp4

GET /health -> { "status": "ok" }
```

Renders run one at a time. A 30-second storyboard takes roughly half a minute on a laptop.

## Run locally

```bash
cd animation-renderer
npm install
npm start
```

The first render downloads Chrome Headless Shell (about 130 MB). Then point the backend worker at it:

```powershell
$env:ANIMATION_RENDERER_URL = "http://localhost:3100"
```

`npm run studio` opens Remotion Studio on the composition for tweaking how scenes look.

## Docker

Build from the repository root, since the image also copies `packages/storyboard/` (`Dockerfile.dockerignore` keeps the rest of the repository out of the build context):

```bash
docker build -f animation-renderer/Dockerfile -t reknit-animation-renderer .
```

## Licence

Remotion is free for individuals and companies of up to three people; larger companies need a [company licence](https://www.remotion.pro/license).
