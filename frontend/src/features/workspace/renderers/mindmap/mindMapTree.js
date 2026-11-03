import ELK from "elkjs/lib/elk.bundled.js";

/**
 * Pure tree helpers for the mind map plus the ELK.js layout. Nodes are
 * { id, label, detail, parentId } with exactly one root (parentId null).
 */

const elk = new ELK();

export const NODE_HEIGHT = 44;
export const ROOT_HEIGHT = 54;

/** Width a node is drawn at; shared by ELK and the node's own style so both agree. */
export function nodeWidth(node, isRoot) {
  const chars = (node.label ?? "").length;
  return Math.round(Math.min(isRoot ? 240 : 210, Math.max(isRoot ? 130 : 96, chars * 7.4 + 44)));
}

export function childrenIndex(nodes) {
  const index = new Map(nodes.map((n) => [n.id, []]));
  for (const n of nodes) {
    if (n.parentId && index.has(n.parentId)) index.get(n.parentId).push(n.id);
  }
  return index;
}

/** True when `candidateId` is `ancestorId` itself or sits anywhere below it. */
export function isInSubtree(nodes, ancestorId, candidateId) {
  const parentOf = new Map(nodes.map((n) => [n.id, n.parentId]));
  for (let id = candidateId; id; id = parentOf.get(id)) {
    if (id === ancestorId) return true;
  }
  return false;
}

/** Nodes whose every ancestor is expanded. */
export function visibleNodes(nodes, rootId, collapsed) {
  const children = childrenIndex(nodes);
  const byId = new Map(nodes.map((n) => [n.id, n]));
  const out = [];
  const walk = (id) => {
    const node = byId.get(id);
    if (!node) return;
    out.push(node);
    if (!collapsed.has(id)) children.get(id).forEach(walk);
  };
  walk(rootId);
  return out;
}

export function removeSubtree(nodes, id) {
  return nodes.filter((n) => !isInSubtree(nodes, id, n.id));
}

/** ELK "layered" left-to-right: the root on the left, each level of branches one column further right. */
export async function layoutTree(nodes, rootId) {
  const ids = new Set(nodes.map((n) => n.id));
  const graph = {
    id: "root",
    layoutOptions: {
      "elk.algorithm": "layered",
      "elk.direction": "RIGHT",
      "elk.layered.spacing.nodeNodeBetweenLayers": "64",
      "elk.spacing.nodeNode": "18",
      "elk.layered.nodePlacement.strategy": "NETWORK_SIMPLEX",
      "elk.layered.considerModelOrder.strategy": "NODES_AND_EDGES",
    },
    children: nodes.map((n) => ({
      id: n.id,
      width: nodeWidth(n, n.id === rootId),
      height: n.id === rootId ? ROOT_HEIGHT : NODE_HEIGHT,
    })),
    edges: nodes
      .filter((n) => n.parentId && ids.has(n.parentId))
      .map((n) => ({ id: `${n.parentId}->${n.id}`, sources: [n.parentId], targets: [n.id] })),
  };
  const result = await elk.layout(graph);
  return new Map(result.children.map((c) => [c.id, { x: c.x, y: c.y }]));
}
