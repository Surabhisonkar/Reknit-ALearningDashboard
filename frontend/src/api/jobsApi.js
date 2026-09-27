import { apiRequest, ApiError } from "./apiClient.js";

/**
 * The frontend's half of "never block on an LLM call": both of these
 * return the moment the backend has enqueued the job (202 Accepted),
 * never once waiting on the model itself. {@link pollJob} is what
 * actually waits, client-side, with a real timeout - so a stuck job
 * fails visibly in the UI instead of spinning forever.
 */
export function submitExplainJob(accessToken, { topic, userNotes }) {
  return apiRequest("POST", "/api/jobs/explain", { accessToken, body: { topic, userNotes } });
}

export function submitVisualizeJob(accessToken, { conceptText, preferredVisualizationType, sourceExplainJobId }) {
  return apiRequest("POST", "/api/jobs/visualize", {
    accessToken,
    body: { conceptText, preferredVisualizationType, sourceExplainJobId },
  });
}

/**
 * Post-save regenerate: the job appends a new version to an existing concept.
 * The server reuses the concept's original input, so only the id is needed.
 */
export function submitRegenerateJob(accessToken, conceptId) {
  return apiRequest("POST", "/api/jobs/visualize", { accessToken, body: { conceptId } });
}

export function getJob(accessToken, jobId) {
  return apiRequest("GET", `/api/jobs/${jobId}`, { accessToken });
}

/**
 * Polls until the job reaches a terminal state (COMPLETED/FAILED) or
 * {@code timeoutMs} elapses. Throws on FAILED (with the backend's
 * user-safe error message) and on timeout, so callers can use a single
 * try/catch rather than branching on status themselves.
 */
export async function pollJob(accessToken, jobId, { intervalMs = 1500, timeoutMs = 120_000 } = {}) {
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const job = await getJob(accessToken, jobId);

    if (job.status === "COMPLETED") return job;
    if (job.status === "FAILED") {
      throw new ApiError(job.errorMessage ?? "Generation failed. Please try again.", 502);
    }

    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new ApiError("This is taking longer than expected. Please try again in a moment.", 504);
}
