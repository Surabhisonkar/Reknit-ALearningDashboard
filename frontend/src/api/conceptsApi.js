import { apiRequest } from "./apiClient.js";

export function listConcepts(accessToken, folder) {
  const query = folder ? `?folder=${encodeURIComponent(folder)}` : "";
  return apiRequest("GET", `/api/concepts${query}`, { accessToken });
}

export function getConcept(accessToken, id) {
  return apiRequest("GET", `/api/concepts/${id}`, { accessToken });
}

export function renameConcept(accessToken, id, title) {
  return apiRequest("PATCH", `/api/concepts/${id}`, { accessToken, body: { title } });
}

export function deleteConcept(accessToken, id) {
  return apiRequest("DELETE", `/api/concepts/${id}`, { accessToken });
}

/** Moves a concept into a folder; folderId null makes it unfiled. Returns the updated concept. */
export function moveConceptToFolder(accessToken, id, folderId) {
  return apiRequest("PUT", `/api/concepts/${id}/folder`, { accessToken, body: { folderId: folderId ?? null } });
}

/**
 * Spark's feed - a random batch of the user's saved animations, optionally
 * scoped to one folder. {@code excludeIds} lets the caller page through the
 * pool without repeats (see ConceptController#sparkFeed on the backend).
 */
export function getSparkFeed(accessToken, { folder, excludeIds = [], limit = 8 } = {}) {
  const params = new URLSearchParams();
  if (folder) params.set("folder", folder);
  if (excludeIds.length > 0) params.set("excludeIds", excludeIds.join(","));
  params.set("limit", String(limit));
  return apiRequest("GET", `/api/concepts/spark-feed?${params.toString()}`, { accessToken });
}

/** Every folder ("library") name the user has saved a concept under - powers the Spark/Library folder pickers. */
export function getConceptFolders(accessToken) {
  return apiRequest("GET", "/api/concepts/folders", { accessToken });
}

/**
 * Confirm-save: turns a completed Visualize job's draft into a saved concept.
 * onDuplicate: "REJECT" (default - a title collision throws ApiError 409 with
 * body.code === "DUPLICATE_TITLE"), "KEEP_BOTH" or "REPLACE". Pass `title` to
 * save under a different name.
 */
export function saveConcept(accessToken, { jobId, title, folder, onDuplicate = "REJECT" }) {
  return apiRequest("POST", "/api/concepts", {
    accessToken,
    body: { jobId, title: title ?? null, folder: folder ?? null, onDuplicate },
  });
}

/** Version metadata only (number, title, createdAt), newest first. */
export function listConceptVersions(accessToken, id) {
  return apiRequest("GET", `/api/concepts/${id}/versions`, { accessToken });
}

/** Full content of one historical version. */
export function getConceptVersion(accessToken, id, version) {
  return apiRequest("GET", `/api/concepts/${id}/versions/${version}`, { accessToken });
}
