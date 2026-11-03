import { createSeedState } from "./seed.js";

/**
 * The demo's "database": one plain object, mirrored to localStorage after
 * every write so a visitor's edits survive a refresh. Every storage access
 * is guarded - private windows and blocked site data fall back to an
 * in-memory copy that simply starts fresh each load.
 */
// Bumped whenever the seed library changes, so returning visitors get the new one.
const STORAGE_KEY = "reknit-demo-db-v3";

function load() {
  try {
    // Earlier seed libraries.
    localStorage.removeItem("reknit-demo-db-v1");
    localStorage.removeItem("reknit-demo-db-v2");
  } catch {
    // storage blocked - nothing to clean up
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // fall through to the seed
  }
  return createSeedState();
}

let state = load();

export function db() {
  return state;
}

export function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // storage full or blocked - the in-memory copy still works for this visit
  }
}

/** Back to the seed library, for the demo banner's "Reset demo" button. */
export function resetDemoData() {
  state = createSeedState();
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // nothing stored to remove
  }
}

export function newId(prefix) {
  return `${prefix}-${crypto.randomUUID()}`;
}
