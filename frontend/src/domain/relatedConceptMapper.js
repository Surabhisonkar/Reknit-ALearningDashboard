/**
 * Maps GET /api/concepts/{id}/related into what the "Related concepts"
 * panel shows. `distance` is deliberately dropped - it's a calibration
 * number for the backend, not something the user should read.
 */
export function mapRelatedConcepts(jsonArray) {
  return (jsonArray ?? [])
    .filter((r) => r && r.id && r.title)
    .map((r) => ({ id: r.id, title: r.title, summary: r.summary ?? "" }));
}
