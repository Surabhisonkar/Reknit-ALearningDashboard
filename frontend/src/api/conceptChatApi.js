import { apiRequest } from "./apiClient.js";

/**
 * Asks the AI about one concept. The server keeps no chat: `history` is the
 * conversation so far ([{ role: "user" | "assistant", text }]). Returns a 202
 * job; poll it - its resultPayload is { kind: "ANSWER", answer }.
 */
export function askConcept(accessToken, conceptId, { history, question }) {
  return apiRequest("POST", `/api/concepts/${conceptId}/ask`, { accessToken, body: { history, question } });
}

/** Condenses a chat into a short note saved on the concept. 202 job; resultPayload { kind: "NOTE", noteId, content }. */
export function saveChatAsNote(accessToken, conceptId, { history }) {
  return apiRequest("POST", `/api/concepts/${conceptId}/notes/from-chat`, { accessToken, body: { history } });
}
