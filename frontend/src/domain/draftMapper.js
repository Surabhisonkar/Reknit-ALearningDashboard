import { mapVisualizationPayload } from "./visualizationMappers.js";

/**
 * Maps a completed Visualize job (resultPayload.kind === "DRAFT") into the
 * shape Capture's draft preview consumes. A draft carries the same
 * `visualization` shape as a saved concept, so the same renderers work on
 * it unchanged. `jobId` is what confirm-save (POST /api/concepts) needs.
 */
export function mapDraft(job) {
  const result = job?.resultPayload;
  if (!result || result.kind !== "DRAFT") {
    throw new Error("This result isn't a reviewable draft.");
  }
  return {
    jobId: job.id,
    title: result.title,
    summary: result.summary,
    suggestedFolder: result.suggestedFolder ?? "",
    visualizationType: result.visualizationType,
    visualization: mapVisualizationPayload(result.visualization),
  };
}
