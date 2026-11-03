import { mkdtemp, readFile, rm } from "node:fs/promises";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

import express from "express";
import { bundle } from "@remotion/bundler";
import { renderMedia, selectComposition } from "@remotion/renderer";

/**
 * POST /render { storyboard, assetUrls } -> video/mp4 bytes.
 *
 * The backend worker calls this after it has validated a storyboard and
 * generated its scene images; it stores the returned MP4 in S3 itself, so
 * this service needs no AWS access. Renders run one at a time.
 */

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const PORT = Number(process.env.PORT ?? 3100);
const MAX_SCENES = 12;

// The composition comes from @reknit/storyboard, a linked folder (../packages/storyboard) with no
// node_modules of its own. Pin remotion/react to this service's copies so the bundle has exactly one of each.
const packageDir = (name) => path.dirname(require.resolve(`${name}/package.json`));
const webpackOverride = (config) => ({
  ...config,
  resolve: {
    ...config.resolve,
    alias: {
      ...(config.resolve?.alias ?? {}),
      remotion: packageDir("remotion"),
      react: packageDir("react"),
      "react-dom": packageDir("react-dom"),
    },
  },
});

console.log("Bundling the storyboard composition...");
const serveUrl = await bundle({ entryPoint: path.join(here, "src/index.js"), webpackOverride });
console.log("Bundle ready.");

let queue = Promise.resolve();
function serialized(task) {
  const run = queue.then(task, task);
  queue = run.catch(() => {});
  return run;
}

async function renderToMp4(inputProps) {
  const composition = await selectComposition({ serveUrl, id: "Storyboard", inputProps });
  const dir = await mkdtemp(path.join(tmpdir(), "reknit-render-"));
  const outputLocation = path.join(dir, "out.mp4");
  try {
    await renderMedia({ composition, serveUrl, codec: "h264", inputProps, outputLocation });
    return await readFile(outputLocation);
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
}

const app = express();
app.use(express.json({ limit: "2mb" }));

app.get("/health", (_req, res) => res.json({ status: "ok" }));

app.post("/render", async (req, res) => {
  const { storyboard, assetUrls = {} } = req.body ?? {};
  if (storyboard?.type !== "animation" || !Array.isArray(storyboard.scenes) || storyboard.scenes.length === 0
      || storyboard.scenes.length > MAX_SCENES) {
    return res.status(400).json({ error: "Expected { storyboard: an animation payload with 1-12 scenes }." });
  }
  const started = Date.now();
  try {
    const video = await serialized(() => renderToMp4({ storyboard, assetUrls }));
    console.log(`Rendered ${storyboard.scenes.length} scenes in ${((Date.now() - started) / 1000).toFixed(1)}s.`);
    res.type("video/mp4").send(video);
  } catch (err) {
    console.error("Render failed:", err);
    res.status(500).json({ error: "Render failed." });
  }
});

app.listen(PORT, () => console.log(`Animation renderer listening on :${PORT}`));
