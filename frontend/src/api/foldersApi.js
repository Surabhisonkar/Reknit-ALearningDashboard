import { apiRequest } from "./apiClient.js";

/** Every folder the user owns, with its concept count. */
export function listFolders(accessToken) {
  return apiRequest("GET", "/api/folders", { accessToken });
}

/** color is optional - omit it and the server picks the least-used palette colour. 409 DUPLICATE_FOLDER_NAME on a name clash. */
export function createFolder(accessToken, { name, color }) {
  return apiRequest("POST", "/api/folders", { accessToken, body: { name, color: color ?? null } });
}

/** Rename and/or recolour; send only what changes. */
export function updateFolder(accessToken, id, { name, color }) {
  const body = {};
  if (name !== undefined) body.name = name;
  if (color !== undefined) body.color = color;
  return apiRequest("PATCH", `/api/folders/${id}`, { accessToken, body });
}

/**
 * Deletes a folder. By default its concepts become unfiled; with
 * deleteConcepts: true they are deleted along with it, in one request.
 */
export function deleteFolder(accessToken, id, { deleteConcepts = false } = {}) {
  const mode = deleteConcepts ? "delete" : "unfile";
  return apiRequest("DELETE", `/api/folders/${id}?concepts=${mode}`, { accessToken });
}
