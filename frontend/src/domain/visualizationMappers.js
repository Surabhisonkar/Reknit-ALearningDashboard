/**
 * Maps the backend's {@code visualization} JSON (see
 * generation.model.VisualizationPayload on the backend) into the shape
 * each renderer component actually consumes. This is the boundary the
 * backend work was building toward: renderer components are keyed
 * strictly off {@code type} and never see raw LLM output or even raw
 * API JSON - only what these functions hand them.
 */

// The subpath keeps Remotion out of the main bundle: the mapper runs on every page, the player only when an animation shows.
import { mapAnimation } from "@reknit/storyboard/mapAnimation";

/**
 * Always hands the renderer a tree: every node has a parentId except the
 * root. Version 2 payloads say so directly; for version 1 the root's own
 * edges become its branches, any node left unattached hangs off the root
 * (adding a root node first if the payload never had one), and the
 * remaining edges stay as cross-links.
 */
function mapMindMap(json) {
  const rawNodes = json.nodes ?? [];
  const nodesById = new Map(rawNodes.map((n) => [n.id, n]));
  const edges = (json.edges ?? []).filter((e) => nodesById.has(e.sourceId) && nodesById.has(e.targetId));
  let rootNodeId = json.layoutHints?.rootNodeId;
  let nodes = rawNodes.map((n) => ({ id: n.id, label: n.label, detail: n.detail ?? "", parentId: n.parentId || null }));

  if (!nodesById.has(rootNodeId)) {
    rootNodeId = "__root";
    nodes = [{ id: rootNodeId, label: json.rootLabel, detail: "", parentId: null }, ...nodes];
  }

  const isTree = (json.version ?? 1) >= 2;
  const treeEdges = new Set();
  if (!isTree) {
    const parentOf = new Map();
    for (const e of edges) {
      if (e.sourceId === rootNodeId && !parentOf.has(e.targetId)) {
        parentOf.set(e.targetId, rootNodeId);
        treeEdges.add(e);
      }
    }
    nodes = nodes.map((n) => (n.id === rootNodeId ? { ...n, parentId: null } : { ...n, parentId: parentOf.get(n.id) ?? rootNodeId }));
  }

  return {
    type: "mind_map",
    version: json.version,
    rootLabel: json.rootLabel,
    nodes,
    crossLinks: edges
      .filter((e) => !treeEdges.has(e))
      .map((e) => ({ sourceId: e.sourceId, targetId: e.targetId, label: e.relationshipLabel ?? "" })),
    rootNodeId,
    citations: (json.citations ?? []).map((c) => ({ nodeId: c.nodeId, sourceText: c.sourceText })),
  };
}

function mapDiagram(json) {
  if (json.syntax === "mermaid") {
    return { type: "diagram", version: json.version, syntax: "mermaid", source: json.source ?? "", caption: json.caption ?? "" };
  }
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
  };
}

function mapImage(json) {
  return {
    type: "image",
    version: json.version,
    imagePrompt: json.imagePrompt,
    artifactId: json.artifactId || null,
    altText: json.altText ?? "",
    labels: (json.labels ?? []).map((l) => ({
      id: l.id,
      text: l.text,
      explanation: l.explanation ?? "",
      x: Math.min(1, Math.max(0, l.x ?? 0.5)),
      y: Math.min(1, Math.max(0, l.y ?? 0.5)),
    })),
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
