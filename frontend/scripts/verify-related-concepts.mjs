#!/usr/bin/env node
/**
 * Step 5 of "RAG as a feature": checks the *product claim* (related
 * concepts are sensible, and unrelated ones stay out), not just that the
 * endpoint returns rows. Runs against your real backend with real Gemini
 * embeddings. See scripts/related-concepts-fixtures.md for what to save first.
 *
 * Usage:
 *   node scripts/verify-related-concepts.mjs --token <access token> [--api http://localhost:8080]
 *        [--id back=<uuid> --id posture=<uuid> ...]
 *
 * Get a token: sign in to the app, then in the browser console run
 *   JSON.parse(sessionStorage.getItem("auth_session")).accessToken
 */

const FIXTURES = [
  { key: "back", label: "Back muscles", group: "body", match: /back\s*muscle|erector|latissimus|rhomboid/i },
  { key: "posture", label: "Posture", group: "body", match: /posture|slouch/i },
  { key: "core", label: "Core stability", group: "body", match: /\bcore\b|transverse abdominis/i },
  { key: "sourdough", label: "Sourdough", group: "control", match: /sourdough|ferment/i },
  { key: "revolution", label: "French Revolution", group: "control", match: /french revolution|1789/i },
];
// must: the vision's own example. should: a softer link. never: body <-> control, control <-> control.
const MUST = [["back", "posture"]];
const SHOULD = [["core", "back"], ["core", "posture"]];

function parseArgs(argv) {
  const args = { api: "http://localhost:8080", token: process.env.LD_TOKEN, ids: {} };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--api") args.api = argv[++i];
    else if (a === "--token") args.token = argv[++i];
    else if (a === "--id") {
      const [k, v] = argv[++i].split("=");
      args.ids[k] = v;
    }
  }
  if (!args.token) {
    console.error("Missing --token (or LD_TOKEN env var). See the header of this file.");
    process.exit(2);
  }
  return args;
}

async function get(args, path) {
  const res = await fetch(`${args.api}${path}`, { headers: { Authorization: `Bearer ${args.token}` } });
  if (!res.ok) throw new Error(`GET ${path} -> ${res.status} ${await res.text()}`);
  return res.json();
}

const fmt = (d) => (d === undefined ? "  -  " : d.toFixed(3));

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const concepts = await get(args, "/api/concepts");

  // 1. Find each fixture concept (by --id, else by keyword on title+summary).
  const found = {};
  for (const f of FIXTURES) {
    const byId = args.ids[f.key] && concepts.find((c) => c.id === args.ids[f.key]);
    const byMatch = concepts.find((c) => f.match.test(`${c.title} ${c.summary}`) && !Object.values(found).some((x) => x.id === c.id));
    const hit = byId || byMatch;
    if (hit) found[f.key] = hit;
    console.log(`${hit ? "found  " : "MISSING"} ${f.label.padEnd(18)} ${hit ? `-> "${hit.title}" (${hit.id})` : "(save it from the fixtures file, or pass --id)"}`);
  }
  const keys = FIXTURES.map((f) => f.key).filter((k) => found[k]);
  if (keys.length < 3 || !found.back || !found.posture) {
    console.error("\nNeed at least Back muscles, Posture and one more fixture saved to say anything meaningful.");
    process.exit(2);
  }

  // 2. What the user actually sees (server defaults) + the full distance matrix (no cut-off).
  const shown = {};
  const dist = {};
  for (const k of keys) {
    const id = found[k].id;
    shown[k] = (await get(args, `/api/concepts/${id}/related`)).map((r) => r.id);
    dist[k] = {};
    for (const r of await get(args, `/api/concepts/${id}/related?limit=20&maxDistance=2`)) dist[k][r.id] = r.distance;
  }
  const d = (a, b) => dist[a]?.[found[b].id] ?? dist[b]?.[found[a].id];
  const isShown = (a, b) => shown[a].includes(found[b].id);
  const indexedless = keys.filter((k) => Object.keys(dist[k]).length === 0);
  if (indexedless.length) {
    console.log(`\nNote: no embedding yet for ${indexedless.join(", ")} - wait a few seconds for INDEX_CONCEPT jobs (check the worker log) and re-run.`);
  }

  console.log("\nCosine distance matrix (lower = more related):");
  console.log("".padEnd(12) + keys.map((k) => k.padStart(11)).join(""));
  for (const a of keys) console.log(a.padEnd(12) + keys.map((b) => (a === b ? "    ·" : fmt(d(a, b))).padStart(11)).join(""));

  console.log("\nWhat each Workspace panel shows right now (server's current threshold):");
  for (const k of keys) {
    const titles = shown[k].map((id) => concepts.find((c) => c.id === id)?.title ?? id);
    console.log(`  ${found[k].title.padEnd(34)} -> ${titles.length ? titles.join(" | ") : "(section hidden)"}`);
  }

  // 3. Checks.
  const results = [];
  const check = (ok, text, level = "MUST") => results.push({ ok, text, level });
  for (const [a, b] of MUST) {
    if (!found[a] || !found[b]) continue;
    check(isShown(a, b) && isShown(b, a), `${found[a].title} and ${found[b].title} appear in each other's panels`);
  }
  const coreLinked = found.core && SHOULD.some(([a, b]) => found[b] && isShown(a, b));
  if (found.core) check(coreLinked, `${found.core.title} links to Back muscles or Posture`, "SHOULD");
  const body = keys.filter((k) => FIXTURES.find((f) => f.key === k).group === "body");
  const controls = keys.filter((k) => FIXTURES.find((f) => f.key === k).group === "control");
  for (const c of controls) {
    check(body.every((b) => !isShown(b, c)), `${found[c].title} stays out of every body-cluster panel`);
    check(shown[c].length === 0, `${found[c].title}'s own panel is hidden (nothing related)`, "SHOULD");
  }

  // 4. Threshold suggestion from the data.
  const positives = [...MUST, ...SHOULD].filter(([a, b]) => found[a] && found[b]).map(([a, b]) => d(a, b)).filter((x) => x !== undefined);
  const negatives = [];
  for (const b of body) for (const c of controls) if (d(b, c) !== undefined) negatives.push(d(b, c));
  for (let i = 0; i < controls.length; i++) for (let j = i + 1; j < controls.length; j++) if (d(controls[i], controls[j]) !== undefined) negatives.push(d(controls[i], controls[j]));
  const mustPos = MUST.filter(([a, b]) => found[a] && found[b]).map(([a, b]) => d(a, b)).filter((x) => x !== undefined);

  console.log("\nChecks:");
  for (const r of results) console.log(`  ${r.ok ? "PASS" : r.level === "MUST" ? "FAIL" : "WARN"}  [${r.level}] ${r.text}`);

  if (positives.length && negatives.length) {
    const worstMust = Math.max(...mustPos);
    const worstPositive = Math.max(...positives);
    const nearestNegative = Math.min(...negatives);
    console.log(`\nCalibration: furthest related pair = ${worstPositive.toFixed(3)} (must-pair ${worstMust.toFixed(3)}), nearest unrelated pair = ${nearestNegative.toFixed(3)}`);
    if (worstMust < nearestNegative) {
      const suggestion = ((worstPositive < nearestNegative ? worstPositive : worstMust) + nearestNegative) / 2;
      console.log(`Embeddings separate the topics. Suggested RELATED_MAX_DISTANCE=${suggestion.toFixed(3)} (halfway between the two).`);
    } else {
      console.log("Embeddings do NOT cleanly separate related from unrelated here - the product claim is at risk; a threshold alone can't fix this (consider task-typed embeddings, see docs).");
    }
  }

  const failed = results.some((r) => !r.ok && r.level === "MUST");
  console.log(failed ? "\nRESULT: product claim NOT met yet." : "\nRESULT: product claim met on this fixture set.");
  process.exit(failed ? 1 : 0);
}

main().catch((e) => {
  console.error(e.message);
  process.exit(2);
});
