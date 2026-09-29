/** Maps GET /api/folders entries into the shape the Library consumes. */
export function mapFolder(json) {
  return {
    id: json.id,
    name: json.name,
    color: json.color,
    conceptCount: json.conceptCount ?? 0,
    createdAt: json.createdAt,
  };
}

/** Sorted by name, case-insensitively, matching the backend's order. */
export function mapFolderList(jsonArray) {
  return (jsonArray ?? [])
    .map(mapFolder)
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: "base" }));
}
