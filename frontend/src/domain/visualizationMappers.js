/**
 * Maps the backend's {@code visualization} JSON (see
 * generation.model.VisualizationPayload on the backend) into the shape
 * each renderer component actually consumes. This is the boundary the
 * backend work was building toward: renderer components are keyed
 * strictly off {@code type} and never see raw LLM output or even raw
 * API JSON - only what these functions hand them.
 */

/**
 * Node positions a person set by dragging: { [nodeId]: {x, y} }, or null.
 * The backend does not store these yet (the "Editable visuals" phase adds
 * it); reading the field now means saved layouts will render the day it
 * does, with no renderer change.
 */
function mapSavedPositions(json) {
  const positions = json.layoutHints?.positions;
  return positions && typeof positions === "object" ? positions : null;
}

function mapMindMap(json) {
  const nodesById = new Map((json.nodes ?? []).map((n) => [n.id, n]));
  return {
    type: "mind_map",
    version: json.version,
    rootLabel: json.rootLabel,
    nodes: (json.nodes ?? []).map((n) => ({ id: n.id, label: n.label, detail: n.detail ?? "" })),
    edges: (json.edges ?? [])
      .filter((e) => nodesById.has(e.sourceId) && nodesById.has(e.targetId))
      .map((e) => ({ sourceId: e.sourceId, targetId: e.targetId, label: e.relationshipLabel ?? "" })),
    orientation: json.layoutHints?.orientation ?? "radial",
    rootNodeId: json.layoutHints?.rootNodeId ?? json.nodes?.[0]?.id ?? null,
    savedPositions: mapSavedPositions(json),
    citations: (json.citations ?? []).map((c) => ({ nodeId: c.nodeId, sourceText: c.sourceText })),
  };
}

function mapDiagram(json) {
  return {
    type: "diagram",
    version: json.version,
    elements: (json.elements ?? []).map((el) => ({
      id: el.id,
      elementType: el.elementType,
      label: el.label,
      x: el.x ?? 0,
      y: el.y ?? 0,
      width: el.width ?? 140,
      height: el.height ?? 60,
      style: el.style ?? "",
      accessibilityLabel: el.accessibilityLabel ?? el.label,
    })),
    connections: (json.connections ?? []).map((c) => ({
      id: c.id,
      sourceId: c.sourceId,
      targetId: c.targetId,
      label: c.label ?? "",
    })),
    savedPositions: mapSavedPositions(json),
  };
}

function mapAnimation(json) {
  const scenes = [...(json.scenes ?? [])].sort((a, b) => a.order - b.order);
  return {
    type: "animation",
    version: json.version,
    scenes: scenes.map((s) => ({
      id: s.id,
      order: s.order,
      title: s.title,
      narration: s.narration,
      durationSeconds: s.durationSeconds ?? 5,
      transition: s.transitionToNext ?? "none",
      assetArtifactIds: s.assetArtifactIds ?? [],
    })),
    totalDurationSeconds: scenes.reduce((sum, s) => sum + (s.durationSeconds ?? 5), 0),
  };
}

function mapImage(json) {
  return {
    type: "image",
    version: json.version,
    imagePrompt: json.imagePrompt,
    artifactId: json.artifactId || null,
    altText: json.altText ?? "",
    // The same idea as a flowchart - shown if the picture can't be, or on "Show as diagram".
    fallbackDiagram: json.fallbackDiagram?.elements?.length ? mapDiagram(json.fallbackDiagram) : null,
  };
}

const MAPPERS = { mind_map: mapMindMap, diagram: mapDiagram, animation: mapAnimation, image: mapImage };

/** Dispatches on {@code json.type}, exactly matching the backend's four visualization types. */
export function mapVisualizationPayload(json) {
  const mapper = MAPPERS[json?.type];
  if (!mapper) {
    throw new Error(`Unknown visualization type: ${json?.type}`);
  }
  return mapper(json);
}
