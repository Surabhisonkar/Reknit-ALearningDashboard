import { mapVisualizationPayload } from "./visualizationMappers.js";

/** Maps a /api/concepts response into the shape pages consume. */
export function mapConcept(json) {
  return {
    id: json.id,
    title: json.title,
    summary: json.summary,
    folder: json.folder ?? "",
    folderId: json.folderId ?? null,
    folderColor: json.folderColor ?? null,
    visualizationType: json.visualizationType,
    visualization: mapVisualizationPayload(json.visualization),
    currentVersion: json.currentVersion ?? 1,
    createdAt: json.createdAt,
    updatedAt: json.updatedAt,
  };
}

/** Maps GET /api/concepts/{id}/versions/{n} - same content fields as a concept, for one historical version. */
export function mapConceptVersion(json) {
  return {
    version: json.version,
    title: json.title,
    summary: json.summary,
    visualizationType: json.visualizationType,
    visualization: mapVisualizationPayload(json.visualization),
    createdAt: json.createdAt,
  };
}

/** Maps the version list, oldest first (the order the dots read left to right). */
export function mapVersionSummaries(jsonArray) {
  return (jsonArray ?? [])
    .map((v) => ({ version: v.version, title: v.title, createdAt: v.createdAt }))
    .sort((a, b) => a.version - b.version);
}

/** Maps a list response - defensively skips any entry that fails to map rather than breaking the whole list. */
export function mapConceptList(jsonArray) {
  return (jsonArray ?? [])
    .map((json) => {
      try {
        return mapConcept(json);
      } catch {
        return null;
      }
    })
    .filter(Boolean);
}
