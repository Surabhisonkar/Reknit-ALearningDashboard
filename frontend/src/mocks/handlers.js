import { delay, http, HttpResponse } from "msw";

import { FOLDER_COLORS } from "../shared/constants/folderColors.js";
import { artifactObjectUrl } from "./artwork.js";
import { db, newId, persist } from "./db.js";
import { answer, artifactFor, condenseChat, explain, keywords, visualize } from "./simulatedAi.js";

/**
 * Mock Service Worker routes mirroring the Spring Boot API one for one:
 * same paths, status codes and JSON shapes (web/dto/*Response), so the
 * app's src/api modules run unchanged. `*` matches any origin.
 */

const API = "*/api";
const JOB_DELAY_MS = [1200, 2600];

// ------------------------------------------------------------------ helpers

const notFound = (what) => HttpResponse.json({ error: `${what} not found.` }, { status: 404 });
const now = () => new Date().toISOString();
const sameName = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();

function findConcept(id) {
  return db().concepts.find((c) => c.id === id);
}

function findFolder(id) {
  return db().folders.find((f) => f.id === id);
}

function toConceptResponse(concept) {
  const current = concept.versions.at(-1);
  const folder = findFolder(concept.folderId);
  return {
    id: concept.id,
    title: current.title,
    summary: current.summary,
    folder: folder?.name ?? "",
    // Omitted when unfiled, like the backend's non_null Jackson setting.
    ...(folder ? { folderId: folder.id, folderColor: folder.color } : {}),
    visualizationType: current.visualizationType,
    visualization: current.visualization,
    currentVersion: current.version,
    createdAt: concept.createdAt,
    updatedAt: concept.updatedAt,
  };
}

function toFolderResponse(folder) {
  return {
    id: folder.id,
    name: folder.name,
    color: folder.color,
    conceptCount: db().concepts.filter((c) => c.folderId === folder.id).length,
    createdAt: folder.createdAt,
  };
}

function conceptsInFolder(folderName) {
  if (!folderName) return db().concepts;
  const folder = db().folders.find((f) => sameName(f.name, folderName));
  return folder ? db().concepts.filter((c) => c.folderId === folder.id) : [];
}

function newestFirst(concepts) {
  return [...concepts].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

function leastUsedColor() {
  const counts = new Map(FOLDER_COLORS.map((c) => [c.key, 0]));
  for (const f of db().folders) counts.set(f.color, (counts.get(f.color) ?? 0) + 1);
  return [...counts.entries()].sort((a, b) => a[1] - b[1])[0][0];
}

function createFolder(name, color) {
  const folder = { id: newId("f"), name: name.trim(), color: color ?? leastUsedColor(), createdAt: now() };
  db().folders.push(folder);
  return folder;
}

function registerArtifact(title) {
  const id = newId("a");
  db().artifacts[id] = artifactFor(title);
  return id;
}

function addVersion(concept, { title, summary, visualizationType, visualization }) {
  concept.versions.push({ version: concept.versions.length + 1, title, summary, visualizationType, visualization, createdAt: now() });
  concept.updatedAt = now();
}

/** Embedding similarity stand-in: keyword overlap between title, summary and source text. */
function relatedTo(concept) {
  const words = (c) => new Set(keywords(`${c.versions.at(-1).title} ${c.versions.at(-1).summary} ${c.sourceText}`));
  const mine = words(concept);
  return db()
    .concepts.filter((c) => c.id !== concept.id)
    .map((c) => {
      const theirs = words(c);
      const shared = [...mine].filter((w) => theirs.has(w)).length;
      return { concept: c, score: shared / (mine.size + theirs.size - shared || 1) };
    })
    .filter((r) => r.score >= 0.06)
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
    .map(({ concept: c, score }) => ({ id: c.id, title: c.versions.at(-1).title, summary: c.versions.at(-1).summary, distance: 1 - score }));
}

// ------------------------------------------------------------------ jobs

/**
 * Jobs live in memory only, like the real queue's in-flight work. A job
 * "finishes" the first time it's polled after its delay - `run` then does
 * what the worker would have done and returns the resultPayload.
 */
const jobs = new Map();

function enqueue(jobType, run, extra = {}) {
  const [min, max] = JOB_DELAY_MS;
  const job = {
    id: newId("j"),
    jobType,
    status: "PENDING",
    resultPayload: null,
    conceptId: extra.conceptId ?? null,
    errorMessage: null,
    createdAt: now(),
    updatedAt: now(),
    readyAt: Date.now() + min + Math.random() * (max - min),
    run,
    ...extra,
  };
  jobs.set(job.id, job);
  return HttpResponse.json(toJobResponse(job), { status: 202 });
}

function toJobResponse(job) {
  const { id, jobType, status, resultPayload, conceptId, errorMessage, createdAt, updatedAt } = job;
  return { id, jobType, status, resultPayload, conceptId, errorMessage, createdAt, updatedAt };
}

function settle(job) {
  if (job.status !== "PENDING" || Date.now() < job.readyAt) return;
  try {
    job.resultPayload = job.run();
    job.status = "COMPLETED";
    persist();
  } catch (err) {
    job.status = "FAILED";
    job.errorMessage = err.message;
  }
  job.updatedAt = now();
}

// ------------------------------------------------------------------ routes

/** Why a mind map edit can't be saved, or null if it can - the checks VisualPayloadValidator runs on the server. */
function mindMapEditProblem(viz, currentType) {
  if (viz?.type !== "mind_map") return "Only mind maps can be edited and saved.";
  if (currentType !== "mind_map") return "An edit can't change the visualization type.";
  const nodes = viz.nodes ?? [];
  if (nodes.length === 0 || nodes.length > 25) return "A mind map needs between 1 and 25 branches.";
  if (nodes.some((n) => !n.label?.trim())) return "Every branch needs a label.";
  const byId = new Map(nodes.map((n) => [n.id, n]));
  const rootId = viz.layoutHints?.rootNodeId;
  if (!byId.has(rootId) || byId.get(rootId).parentId) return "The mind map has lost its main idea.";
  for (const node of nodes) {
    const seen = new Set();
    let at = node;
    while (at && at.id !== rootId) {
      if (seen.has(at.id)) return "Branches can't loop back on themselves.";
      seen.add(at.id);
      at = byId.get(at.parentId);
    }
    if (!at) return "Every branch has to connect back to the main idea.";
  }
  return null;
}

export const handlers = [
  // ---- account (the demo user is always signed in)
  http.get(`${API}/account`, () =>
    HttpResponse.json({ id: "demo-user", email: "demo@example.com", displayName: "Demo Learner", deletionPending: false })
  ),
  http.put(`${API}/account/profile`, () => new HttpResponse(null, { status: 204 })),
  http.delete(`${API}/account`, () => HttpResponse.json({ error: "Account deletion is turned off in the demo." }, { status: 403 })),
  http.post(`${API}/account/cancel-deletion`, () => new HttpResponse(null, { status: 204 })),

  // ---- uploads: generated images come back as local object URLs instead of presigned S3 links
  http.get(`${API}/artifacts/:id`, ({ params }) => {
    const artifact = db().artifacts[params.id];
    if (!artifact) return notFound("Artifact");
    return HttpResponse.json({ downloadUrl: artifactObjectUrl(params.id, artifact), expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
  }),

  // ---- folders
  http.get(`${API}/folders`, async () => {
    await delay(120);
    return HttpResponse.json(db().folders.map(toFolderResponse));
  }),
  http.post(`${API}/folders`, async ({ request }) => {
    const { name, color } = await request.json();
    if (!name?.trim()) return HttpResponse.json({ error: "Folder name is required." }, { status: 400 });
    if (db().folders.some((f) => sameName(f.name, name))) {
      return HttpResponse.json({ error: `You already have a folder called "${name.trim()}".`, code: "DUPLICATE_FOLDER_NAME" }, { status: 409 });
    }
    const folder = createFolder(name, color);
    persist();
    return HttpResponse.json(toFolderResponse(folder), { status: 201 });
  }),
  http.patch(`${API}/folders/:id`, async ({ params, request }) => {
    const folder = findFolder(params.id);
    if (!folder) return notFound("Folder");
    const { name, color } = await request.json();
    if (name !== undefined) {
      if (db().folders.some((f) => f.id !== folder.id && sameName(f.name, name))) {
        return HttpResponse.json({ error: `You already have a folder called "${name.trim()}".`, code: "DUPLICATE_FOLDER_NAME" }, { status: 409 });
      }
      folder.name = name.trim();
    }
    if (color !== undefined) folder.color = color;
    persist();
    return HttpResponse.json(toFolderResponse(folder));
  }),
  http.delete(`${API}/folders/:id`, ({ params, request }) => {
    const folder = findFolder(params.id);
    if (!folder) return notFound("Folder");
    const state = db();
    if (new URL(request.url).searchParams.get("concepts") === "delete") {
      const doomed = new Set(state.concepts.filter((c) => c.folderId === folder.id).map((c) => c.id));
      state.concepts = state.concepts.filter((c) => !doomed.has(c.id));
      state.notes = state.notes.filter((n) => !doomed.has(n.conceptId));
    } else {
      for (const c of state.concepts) if (c.folderId === folder.id) c.folderId = null;
    }
    state.folders = state.folders.filter((f) => f.id !== folder.id);
    persist();
    return new HttpResponse(null, { status: 204 });
  }),

  // ---- concepts: literal paths first, ahead of /:id
  http.get(`${API}/concepts/spark-feed`, async ({ request }) => {
    await delay(150);
    const params = new URL(request.url).searchParams;
    const exclude = new Set((params.get("excludeIds") ?? "").split(",").filter(Boolean));
    const limit = Math.min(Math.max(Number(params.get("limit") ?? 10), 1), 25);
    const pool = conceptsInFolder(params.get("folder")).filter((c) => !exclude.has(c.id));
    const shuffled = [...pool].sort(() => Math.random() - 0.5).slice(0, limit);
    return HttpResponse.json(shuffled.map(toConceptResponse));
  }),
  http.get(`${API}/concepts/folders`, () => {
    const used = new Set(db().concepts.map((c) => c.folderId));
    return HttpResponse.json(db().folders.filter((f) => used.has(f.id)).map((f) => f.name).sort((a, b) => a.localeCompare(b)));
  }),
  http.get(`${API}/concepts`, async ({ request }) => {
    await delay(150);
    const folder = new URL(request.url).searchParams.get("folder");
    return HttpResponse.json(newestFirst(conceptsInFolder(folder)).map(toConceptResponse));
  }),
  http.post(`${API}/concepts`, async ({ request }) => {
    const { jobId, title: requestedTitle, folder: folderName, onDuplicate = "REJECT" } = await request.json();
    const job = jobs.get(jobId);
    if (!job || job.status !== "COMPLETED" || job.resultPayload?.kind !== "DRAFT") {
      return HttpResponse.json({ error: "That draft is no longer available. Generate it again." }, { status: 404 });
    }
    const existing = job.savedConceptId && findConcept(job.savedConceptId);
    if (existing) return HttpResponse.json(toConceptResponse(existing)); // idempotent re-save

    const draft = job.resultPayload;
    let title = requestedTitle?.trim() || draft.title;
    const state = db();
    const duplicate = state.concepts.find((c) => sameName(c.versions.at(-1).title, title));
    const content = { title, summary: draft.summary, visualizationType: draft.visualizationType, visualization: draft.visualization };

    if (duplicate && onDuplicate === "REJECT") {
      return HttpResponse.json(
        { error: `You already have a concept called "${title}".`, code: "DUPLICATE_TITLE", duplicateConceptId: duplicate.id },
        { status: 409 }
      );
    }
    if (duplicate && onDuplicate === "REPLACE") {
      addVersion(duplicate, content);
      job.savedConceptId = duplicate.id;
      persist();
      return HttpResponse.json(toConceptResponse(duplicate));
    }
    if (duplicate) {
      let n = 2;
      while (state.concepts.some((c) => sameName(c.versions.at(-1).title, `${title} (${n})`))) n++;
      title = `${title} (${n})`;
    }

    let folderId = null;
    if (folderName?.trim()) {
      folderId = (state.folders.find((f) => sameName(f.name, folderName)) ?? createFolder(folderName)).id;
    }
    const concept = {
      id: newId("c"),
      folderId,
      sourceText: job.sourceText,
      versions: [{ ...content, title, version: 1, createdAt: now() }],
      createdAt: now(),
      updatedAt: now(),
    };
    state.concepts.push(concept);
    job.savedConceptId = concept.id;
    persist();
    return HttpResponse.json(toConceptResponse(concept), { status: 201 });
  }),
  http.get(`${API}/concepts/:id`, async ({ params }) => {
    await delay(120);
    const concept = findConcept(params.id);
    return concept ? HttpResponse.json(toConceptResponse(concept)) : notFound("Concept");
  }),
  http.patch(`${API}/concepts/:id`, async ({ params, request }) => {
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    const { title } = await request.json();
    if (!title?.trim()) return HttpResponse.json({ error: "Title is required." }, { status: 400 });
    concept.versions.at(-1).title = title.trim();
    concept.updatedAt = now();
    persist();
    return HttpResponse.json(toConceptResponse(concept));
  }),
  http.delete(`${API}/concepts/:id`, ({ params }) => {
    const state = db();
    if (!findConcept(params.id)) return notFound("Concept");
    state.concepts = state.concepts.filter((c) => c.id !== params.id);
    state.notes = state.notes.filter((n) => n.conceptId !== params.id);
    persist();
    return new HttpResponse(null, { status: 204 });
  }),
  // Mirrors the backend's VisualizationEditService: mind maps only, same tree rules, saved as the next version.
  http.put(`${API}/concepts/:id/visualization`, async ({ params, request }) => {
    await delay(250);
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    const { visualization } = await request.json();
    const current = concept.versions.at(-1);
    const problem = mindMapEditProblem(visualization, current.visualizationType);
    if (problem) return HttpResponse.json({ error: problem, details: [] }, { status: 400 });
    concept.versions.push({ ...current, version: current.version + 1, visualization, createdAt: now() });
    concept.updatedAt = now();
    persist();
    return HttpResponse.json(toConceptResponse(concept));
  }),
  http.put(`${API}/concepts/:id/folder`, async ({ params, request }) => {
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    const { folderId } = await request.json();
    if (folderId && !findFolder(folderId)) return notFound("Folder");
    concept.folderId = folderId ?? null;
    persist();
    return HttpResponse.json(toConceptResponse(concept));
  }),
  http.get(`${API}/concepts/:id/versions`, ({ params }) => {
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    return HttpResponse.json([...concept.versions].reverse().map(({ version, title, createdAt }) => ({ version, title, createdAt })));
  }),
  http.get(`${API}/concepts/:id/versions/:version`, ({ params }) => {
    const v = findConcept(params.id)?.versions.find((x) => x.version === Number(params.version));
    if (!v) return notFound("Version");
    const { version, title, summary, visualizationType, visualization, createdAt } = v;
    // The demo keeps one source text per concept: every version (regenerates, edits) is drawn from it.
    const sourceText = findConcept(params.id).sourceText ?? null;
    return HttpResponse.json({ version, title, summary, visualizationType, visualization, createdAt, sourceText });
  }),
  http.get(`${API}/concepts/:id/related`, async ({ params }) => {
    await delay(200);
    const concept = findConcept(params.id);
    return concept ? HttpResponse.json(relatedTo(concept)) : notFound("Concept");
  }),

  // ---- notes and Spark chat
  http.get(`${API}/concepts/:id/notes`, ({ params }) => {
    const notes = db().notes.filter((n) => n.conceptId === params.id);
    return HttpResponse.json(
      notes.sort((a, b) => b.createdAt.localeCompare(a.createdAt)).map(({ id, content, source, createdAt }) => ({ id, content, source, createdAt }))
    );
  }),
  http.delete(`${API}/concepts/:id/notes/:noteId`, ({ params }) => {
    db().notes = db().notes.filter((n) => n.id !== params.noteId);
    persist();
    return new HttpResponse(null, { status: 204 });
  }),
  http.post(`${API}/concepts/:id/ask`, async ({ params, request }) => {
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    const { question } = await request.json();
    return enqueue("ASK_CONCEPT", () => ({ kind: "ANSWER", answer: answer(toConceptResponse(concept), question) }), { conceptId: concept.id });
  }),
  http.post(`${API}/concepts/:id/notes/from-chat`, async ({ params, request }) => {
    const concept = findConcept(params.id);
    if (!concept) return notFound("Concept");
    const { history } = await request.json();
    return enqueue(
      "CHAT_TO_NOTE",
      () => {
        const note = { id: newId("n"), conceptId: concept.id, source: "chat", createdAt: now(), content: condenseChat(toConceptResponse(concept), history ?? []) };
        db().notes.push(note);
        return { kind: "NOTE", noteId: note.id, content: note.content };
      },
      { conceptId: concept.id }
    );
  }),

  // ---- generation jobs
  http.post(`${API}/jobs/explain`, async ({ request }) => {
    const { topic, userNotes } = await request.json();
    if (!topic?.trim()) return HttpResponse.json({ error: "Topic is required." }, { status: 400 });
    return enqueue("EXPLAIN", () => explain(topic, userNotes));
  }),
  http.post(`${API}/jobs/visualize`, async ({ request }) => {
    const { conceptText, preferredVisualizationType, conceptId } = await request.json();

    if (conceptId) {
      // Regenerate: append version N+1 from the concept's original input, same visual type.
      const concept = findConcept(conceptId);
      if (!concept) return notFound("Concept");
      return enqueue(
        "VISUALIZE",
        () => {
          const current = concept.versions.at(-1);
          const result = visualize(concept.sourceText, current.visualizationType, registerArtifact, concept.versions.length);
          addVersion(concept, { ...result, title: current.title });
          return { kind: "VERSION", version: concept.versions.length };
        },
        { conceptId }
      );
    }

    if (!conceptText?.trim()) return HttpResponse.json({ error: "Concept text is required." }, { status: 400 });
    return enqueue(
      "VISUALIZE",
      () => ({ kind: "DRAFT", suggestedFolder: "", ...visualize(conceptText, preferredVisualizationType, registerArtifact) }),
      { sourceText: conceptText }
    );
  }),
  http.get(`${API}/jobs/:id`, ({ params }) => {
    const job = jobs.get(params.id);
    if (!job) return notFound("Job");
    settle(job);
    return HttpResponse.json(toJobResponse(job));
  }),
];
