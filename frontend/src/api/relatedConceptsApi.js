import { apiRequest } from "./apiClient.js";

/**
 * Concepts the user already saved that relate to this one (across folders),
 * nearest first. May be an empty list - e.g. nothing close enough, or the
 * concept was only just saved and hasn't been indexed yet.
 */
export function getRelatedConcepts(accessToken, conceptId) {
  return apiRequest("GET", `/api/concepts/${conceptId}/related`, { accessToken });
}
