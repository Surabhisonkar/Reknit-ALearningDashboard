#!/usr/bin/env node
/**
 * Phase 7 (Folders + Library) end-to-end check against your REAL backend,
 * real database and real AI - no mocks. Drives the actual Library UI with
 * Playwright and cross-checks every step through the API.
 *
 * USE A SEPARATE TEST ACCOUNT. The script creates 2 concepts through the real
 * Visualize pipeline (uses some Gemini quota, ~1-2 min each), plus a few
 * folders, and deletes everything it created at the end - even when a check
 * fails. It never modifies or deletes anything that existed before it started.
 *
 * Setup (once):
 *   npm install                       # installs the pinned playwright devDependency
 *   npx playwright install chromium   # downloads the browser
 *
 * Run (backend api + worker running, and `npm run dev` pointed at the same api):
 *   node scripts/verify-phase7-folders.mjs --test-account --token <access token>
 *        [--api http://localhost:8080] [--app http://localhost:5173] [--headed]
 *
 * Get the token: sign in to the app AS THE TEST ACCOUNT, then in the browser console run
 *   JSON.parse(sessionStorage.getItem("auth_session")).accessToken
 */
import { chromium } from "playwright";

// ---------------------------------------------------------------- arguments
function parseArgs(argv) {
  const args = { api: "http://localhost:8080", app: "http://localhost:5173", token: process.env.LD_TOKEN, headed: false, testAccount: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--api") args.api = argv[++i];
    else if (a === "--app") args.app = argv[++i];
    else if (a === "--token") args.token = argv[++i];
    else if (a === "--headed") args.headed = true;
    else if (a === "--test-account") args.testAccount = true;
  }
  if (!args.testAccount) {
    console.error("Refusing to run without --test-account: this script creates and deletes data. Use a separate test account.");
    process.exit(2);
  }
  if (!args.token) {
    console.error("Missing --token (or LD_TOKEN env var). See the header of this file.");
    process.exit(2);
  }
  return args;
}

const args = parseArgs(process.argv.slice(2));
const RUN = Date.now().toString(36).slice(-5);
const names = {
  auto: `P7 Auto ${RUN}`,
  ui: `P7 UI ${RUN}`,
  uiRenamed: `P7 UI renamed ${RUN}`,
  move: `P7 Move ${RUN}`,
};

// ---------------------------------------------------------------- API helpers
async function api(method, path, body) {
  const res = await fetch(`${args.api}${path}`, {
    method,
    headers: { Authorization: `Bearer ${args.token}`, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = text;
  }
  return { status: res.status, json };
}

async function apiOk(method, path, body) {
  const r = await api(method, path, body);
  if (r.status >= 400) throw new Error(`${method} ${path} -> ${r.status} ${JSON.stringify(r.json)}`);
  return r.json;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function visualizeAndSave(conceptText, folder) {
  const job = await apiOk("POST", "/api/jobs/visualize", { conceptText, preferredVisualizationType: "mind_map" });
  const deadline = Date.now() + 4 * 60_000;
  for (;;) {
    const j = await apiOk("GET", `/api/jobs/${job.id}`);
    if (j.status === "COMPLETED") break;
    if (j.status === "FAILED") throw new Error(`Visualize job failed: ${j.errorMessage}`);
    if (Date.now() > deadline) throw new Error("Visualize job took longer than 4 minutes");
    await sleep(3000);
  }
  return apiOk("POST", "/api/concepts", { jobId: job.id, folder, onDuplicate: "KEEP_BOTH" });
}

// ---------------------------------------------------------------- tiny test runner
const results = [];
async function check(name, fn) {
  try {
    await fn();
    results.push(["PASS", name]);
  } catch (e) {
    results.push(["FAIL", name, e.message.split("\n")[0]]);
  }
}
function expect(cond, message) {
  if (!cond) throw new Error(message);
}

// ---------------------------------------------------------------- state for cleanup
const created = { conceptIds: new Set(), folderIdsBefore: new Set() };

async function cleanup() {
  for (const id of created.conceptIds) {
    const r = await api("DELETE", `/api/concepts/${id}`);
    if (r.status !== 204 && r.status !== 404) console.warn(`  cleanup: DELETE concept ${id} -> ${r.status}`);
  }
  const folders = (await api("GET", "/api/folders")).json ?? [];
  for (const f of folders) {
    if (!created.folderIdsBefore.has(f.id) && f.name.includes(RUN)) {
      const r = await api("DELETE", `/api/folders/${f.id}?concepts=unfile`);
      if (r.status !== 204) console.warn(`  cleanup: DELETE folder ${f.name} -> ${r.status}`);
    }
  }
}

// ---------------------------------------------------------------- main
async function main() {
  (await apiOk("GET", "/api/folders")).forEach((f) => created.folderIdsBefore.add(f.id));
  console.log(`Run ${RUN}: creating 2 concepts through the real Visualize pipeline (this takes a minute or two)...`);

  // Two concepts saved into the same new folder, spelled in different letter case:
  // the second save must reuse the first's folder (case-insensitive find-or-create).
  const a = await visualizeAndSave("The Pomodoro technique: 25 minutes of focus, then a 5 minute break.", names.auto);
  created.conceptIds.add(a.id);
  const b = await visualizeAndSave("Box breathing: inhale 4s, hold 4s, exhale 4s, hold 4s to calm down.", names.auto.toUpperCase());
  created.conceptIds.add(b.id);

  // ------------------------------------------------ API contract checks
  await check("API: auto-filing creates the folder once and reuses it case-insensitively", async () => {
    expect(a.folderId && a.folderId === b.folderId, `expected one folder, got ${a.folderId} / ${b.folderId}`);
    expect(a.folder === names.auto && b.folder === names.auto, `folder names: ${a.folder} / ${b.folder}`);
    expect(typeof a.folderColor === "string" && a.folderColor.length > 0, "folderColor missing");
  });
  await check("API: GET /api/folders lists it with conceptCount 2", async () => {
    const f = (await apiOk("GET", "/api/folders")).find((x) => x.id === a.folderId);
    expect(f && f.conceptCount === 2, `got ${JSON.stringify(f)}`);
  });
  await check("API: legacy ?folder= filter still works, in any letter case", async () => {
    const list = await apiOk("GET", `/api/concepts?folder=${encodeURIComponent(names.auto.toLowerCase())}`);
    expect(list.length === 2, `got ${list.length}`);
  });
  await check("API: legacy /api/concepts/folders still lists names (non-empty folders only)", async () => {
    const list = await apiOk("GET", "/api/concepts/folders");
    expect(list.includes(names.auto), `missing ${names.auto}`);
  });
  await check("API: duplicate folder name in another case -> 409 DUPLICATE_FOLDER_NAME", async () => {
    const r = await api("POST", "/api/folders", { name: names.auto.toLowerCase() });
    expect(r.status === 409 && r.json?.code === "DUPLICATE_FOLDER_NAME", `got ${r.status} ${JSON.stringify(r.json)}`);
  });
  await check("API: unknown colour -> 400", async () => {
    const r = await api("POST", "/api/folders", { name: `P7 Bad ${RUN}`, color: "hotpink" });
    expect(r.status === 400, `got ${r.status}`);
  });
  await check("API: DELETE folder without ?concepts= (or with a bad value) -> 400, nothing deleted", async () => {
    const missing = await api("DELETE", `/api/folders/${a.folderId}`);
    const bad = await api("DELETE", `/api/folders/${a.folderId}?concepts=all`);
    expect(missing.status === 400 && bad.status === 400, `got ${missing.status} / ${bad.status}`);
    expect((await api("GET", `/api/concepts/${a.id}`)).status === 200, "concept A should still exist");
  });
  await check("API: moving into a folder that isn't yours -> 404", async () => {
    const r = await api("PUT", `/api/concepts/${a.id}/folder`, { folderId: "00000000-0000-4000-8000-000000000000" });
    expect(r.status === 404, `got ${r.status}`);
  });

  // ------------------------------------------------ UI checks (real API)
  const browser = await chromium.launch({ headless: !args.headed });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 860 } });
  await ctx.addInitScript((token) => {
    sessionStorage.setItem("auth_session", JSON.stringify({ accessToken: token, idToken: "x.e30.y", refreshToken: "", expiresAtMs: Date.now() + 50 * 60_000 }));
  }, args.token);
  const page = await ctx.newPage();
  const pageErrors = [];
  page.on("pageerror", (e) => pageErrors.push(e.message));

  const tile = (name) => page.locator(".folder-tile", { hasText: name });
  const openFolder = async (name) => {
    if (await tile(name).evaluate((el) => el.inert)) await page.locator(".folder-more").click();
    await tile(name).click();
  };
  const cardActions = (title) => page.getByRole("button", { name: `Actions for ${title}`, exact: true });

  try {
    await page.goto(`${args.app}/library`);
    await page.locator(".library-card").first().waitFor({ timeout: 20_000 });

    await check("UI: the auto-created folder shows with its count", async () => {
      await tile(names.auto).waitFor({ state: "attached" });
      expect((await tile(names.auto).locator(".folder-tile-count").textContent()).trim() === "2", "count is not 2");
    });

    await check("UI: + creates a folder with a chosen colour; duplicate name errors inline", async () => {
      await page.getByRole("button", { name: "New folder" }).first().click();
      const dialog = page.getByRole("dialog", { name: "New folder" });
      await dialog.getByLabel("Name").fill(names.auto.toLowerCase());
      await dialog.getByRole("button", { name: "Create folder" }).click();
      await dialog.getByRole("alert").waitFor();
      await dialog.getByLabel("Name").fill(names.ui);
      await dialog.locator(".swatch", { has: page.locator('input[value="rose"]') }).click();
      await dialog.getByRole("button", { name: "Create folder" }).click();
      await dialog.waitFor({ state: "detached" });
      await tile(names.ui).waitFor({ state: "attached" });
      const f = (await apiOk("GET", "/api/folders")).find((x) => x.name === names.ui);
      expect(f && f.color === "rose", `server has ${JSON.stringify(f)}`);
    });

    await check("UI: rename + recolour", async () => {
      await openFolder(names.ui);
      await page.getByRole("button", { name: `Actions for folder ${names.ui}` }).click();
      await page.getByRole("menuitem", { name: "Rename or recolour" }).click();
      const dialog = page.getByRole("dialog", { name: "Edit folder" });
      await dialog.getByLabel("Name").fill(names.uiRenamed);
      await dialog.locator(".swatch", { has: page.locator('input[value="green"]') }).click();
      await dialog.getByRole("button", { name: "Save changes" }).click();
      await dialog.waitFor({ state: "detached" });
      const f = (await apiOk("GET", "/api/folders")).find((x) => x.name === names.uiRenamed);
      expect(f && f.color === "green", `server has ${JSON.stringify(f)}`);
    });

    await check("UI: move concept A into the renamed folder", async () => {
      await page.locator(".folder-tile", { hasText: "All concepts" }).click();
      await cardActions(a.title).click();
      await page.getByRole("menuitem", { name: "Move to folder" }).click();
      const dialog = page.getByRole("dialog");
      await dialog.getByRole("button", { name: names.uiRenamed, exact: true }).click();
      await dialog.waitFor({ state: "detached" });
      const now = await apiOk("GET", `/api/concepts/${a.id}`);
      expect(now.folder === names.uiRenamed, `concept A is in "${now.folder}"`);
    });

    await check("UI: move dialog + creates a folder (with colour) and moves concept B into it", async () => {
      await cardActions(b.title).click();
      await page.getByRole("menuitem", { name: "Move to folder" }).click();
      const dialog = page.getByRole("dialog");
      await dialog.getByRole("button", { name: "New folder" }).click();
      await dialog.getByLabel("New folder name").fill(names.move);
      await dialog.locator(".swatch", { has: page.locator('input[value="sky"]') }).click();
      await dialog.getByRole("button", { name: "Create and move" }).click();
      await dialog.waitFor({ state: "detached" });
      const now = await apiOk("GET", `/api/concepts/${b.id}`);
      expect(now.folder === names.move && now.folderColor === "sky", `concept B: ${now.folder} / ${now.folderColor}`);
    });

    await check("UI: search finds concept A by its title", async () => {
      await page.getByLabel("Search your library").fill(a.title);
      await page.getByRole("heading", { name: a.title, exact: true }).waitFor();
      await page.getByLabel("Search your library").fill("");
    });

    await check("UI: delete folder, keep concepts -> concept A becomes unfiled, still exists", async () => {
      await openFolder(names.uiRenamed);
      await page.getByRole("button", { name: `Actions for folder ${names.uiRenamed}` }).click();
      await page.getByRole("menuitem", { name: "Delete folder" }).click();
      await page.getByRole("button", { name: "Delete folder", exact: true }).click();
      await page.getByRole("dialog").waitFor({ state: "detached" });
      const now = await apiOk("GET", `/api/concepts/${a.id}`);
      expect(now.folder === "" && !now.folderId, `concept A: ${JSON.stringify(now.folder)}`);
    });

    await check("UI: delete folder with its concepts -> concept B is gone", async () => {
      await openFolder(names.move);
      await page.getByRole("button", { name: `Actions for folder ${names.move}` }).click();
      await page.getByRole("menuitem", { name: "Delete folder" }).click();
      await page.getByLabel(/Delete the concepts too/).check();
      await page.getByRole("button", { name: "Delete folder and 1 concept" }).click();
      await page.getByRole("dialog").waitFor({ state: "detached" });
      expect((await api("GET", `/api/concepts/${b.id}`)).status === 404, "concept B still exists");
      created.conceptIds.delete(b.id);
    });

    await check("UI + API: the now-empty auto folder drops out of the legacy name list (Spark never offers it)", async () => {
      const list = await apiOk("GET", "/api/concepts/folders");
      expect(!list.includes(names.auto), `${names.auto} is still listed`);
    });

    await check("UI: no page errors", async () => expect(pageErrors.length === 0, pageErrors.join(" | ")));
  } finally {
    await browser.close();
  }
}

try {
  await main();
} catch (e) {
  results.push(["FAIL", "setup", e.message]);
} finally {
  console.log("Cleaning up everything this run created...");
  await cleanup();
}

for (const r of results) console.log(r.join("  "));
const failed = results.filter((r) => r[0] === "FAIL").length;
console.log(`\n${results.length - failed}/${results.length} passed`);
process.exit(failed ? 1 : 0);
