import { apiRequest } from "./apiClient.js";

/** Notes saved on a concept, newest first: [{ id, content, source, createdAt }]. */
export function listConceptNotes(accessToken, conceptId) {
  return apiRequest("GET", `/api/concepts/${conceptId}/notes`, { accessToken });
}

export function deleteConceptNote(accessToken, conceptId, noteId) {
  return apiRequest("DELETE", `/api/concepts/${conceptId}/notes/${noteId}`, { accessToken });
}
