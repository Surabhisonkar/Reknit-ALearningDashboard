/**
 * Pure layout functions: mapped visualization in, React Flow nodes/edges
 * out. No React, no DOM - so they can be unit-checked with plain Node and
 * reused by any future canvas.
 *
 * Positions are always computed here, never taken from the AI: models
 * are unreliable at coordinates (overlaps, everything at 0,0), and the
 * rule-based failsafe has no layout sense at all. The one exception is
 * `savedPositions` - positions a person set by dragging, which a later
 * phase will persist; when present they win over the computed layout.
 */

export const MIND_NODE = { width: 170, height: 48 };
export const DIAGRAM_NODE = { width: 200, height: 64 };

const RING_GAP_MIN = 190;
const RING_GAP_PER_LEAF = 26;
/** Nodes are wider than tall, so rings are stretched sideways to keep labels apart. */
const RING_X_STRETCH = 1.35;
const TREE_LEVEL_GAP = 240;
const TREE_SLOT_GAP = 76;
/** Compact (phone) mind map: an indented outline, one node per row. */
const OUTLINE_INDENT = 40;
const OUTLINE_ROW_GAP = 66;

const DIAGRAM_COLUMN_GAP = 250;
const DIAGRAM_ROW_GAP = 130;
/** A long single chain is folded into rows this wide instead of one tall column. */
const CHAIN_PER_ROW = 4;
const CHAIN_FOLD_FROM = 6;
const COMPACT_CHAIN_PER_ROW = 2;
const COMPACT_CHAIN_FOLD_FROM = 4;
const COMPACT_DIAGRAM_ROW_GAP = 104;

function applySavedPositions(nodes, savedPositions) {
  if (!savedPositions) return nodes;
  return nodes.map((node) => {
    const saved = savedPositions[node.id];
    return saved && Number.isFinite(saved.x) && Number.isFinite(saved.y)
      ? { ...node, position: { x: saved.x, y: saved.y } }
      : node;
  });
}

// ---- mind map -------------------------------------------------------------

/**
 * Turns the node/edge graph into a spanning tree rooted at `rootId`
 * (breadth-first, so each node hangs off its nearest route to the root).
 * Nodes no edge reaches are attached straight to the root - that is what
 * an edge-less AI answer means ("everything branches from the centre").
 */
function spanningTree(nodes, edges, rootId) {
  const neighbours = new Map(nodes.map((n) => [n.id, []]));
  edges.forEach((e) => {
    neighbours.get(e.sourceId)?.push(e.targetId);
    neighbours.get(e.targetId)?.push(e.sourceId);
  });

  const parent = new Map([[rootId, null]]);
  const children = new Map(nodes.map((n) => [n.id, []]));
  const depth = new Map([[rootId, 0]]);

  function spread(startId) {
    const queue = [startId];
    while (queue.length > 0) {
      const id = queue.shift();
      for (const next of neighbours.get(id) ?? []) {
        if (parent.has(next)) continue;
        parent.set(next, id);
        depth.set(next, depth.get(id) + 1);
        children.get(id).push(next);
        queue.push(next);
      }
    }
  }

  spread(rootId);
  nodes.forEach((n) => {
    if (parent.has(n.id)) return;
    parent.set(n.id, rootId);
    depth.set(n.id, 1);
    children.get(rootId).push(n.id);
    spread(n.id);
  });

  return { parent, children, depth };
}

function countLeaves(id, children, cache = new Map()) {
  if (cache.has(id)) return cache.get(id);
  const kids = children.get(id) ?? [];
  const leaves = kids.length === 0 ? 1 : kids.reduce((sum, kid) => sum + countLeaves(kid, children, cache), 0);
  cache.set(id, leaves);
  return leaves;
}

function radialCentres(rootId, children, depth) {
  const leafCache = new Map();
  const ringGap = Math.max(RING_GAP_MIN, countLeaves(rootId, children, leafCache) * RING_GAP_PER_LEAF);
  const centres = new Map([[rootId, { x: 0, y: 0 }]]);

  function place(id, startAngle, endAngle) {
    const kids = children.get(id) ?? [];
    const total = kids.reduce((sum, kid) => sum + countLeaves(kid, children, leafCache), 0);
    let cursor = startAngle;
    kids.forEach((kid) => {
      const span = ((endAngle - startAngle) * countLeaves(kid, children, leafCache)) / total;
      const angle = cursor + span / 2;
      const radius = depth.get(kid) * ringGap;
      centres.set(kid, { x: radius * RING_X_STRETCH * Math.cos(angle), y: radius * Math.sin(angle) });
      place(kid, cursor, cursor + span);
      cursor += span;
    });
  }

  // Start at 12 o'clock so the first branch reads first.
  place(rootId, -Math.PI / 2, (3 * Math.PI) / 2);
  return centres;
}

/** A tidy tree: leaves take consecutive slots, every parent sits centred over its children. */
function treeCentres(rootId, children, depth, horizontal) {
  const centres = new Map();
  let nextSlot = 0;

  function place(id) {
    const kids = children.get(id) ?? [];
    let slot;
    if (kids.length === 0) {
      slot = nextSlot++;
    } else {
      const slots = kids.map(place);
      slot = (slots[0] + slots[slots.length - 1]) / 2;
    }
    const along = depth.get(id) * TREE_LEVEL_GAP;
    const across = slot * (horizontal ? TREE_SLOT_GAP : MIND_NODE.width + 30);
    centres.set(id, horizontal ? { x: along, y: across } : { x: across, y: along * 0.6 });
    return slot;
  }

  place(rootId);
  return centres;
}

/** Depth-first, one row per node, each level indented - reads top to bottom on a narrow screen. */
function outlineCentres(rootId, children, depth) {
  const centres = new Map();
  let row = 0;

  function place(id) {
    centres.set(id, { x: depth.get(id) * OUTLINE_INDENT, y: row++ * OUTLINE_ROW_GAP });
    (children.get(id) ?? []).forEach(place);
  }

  place(rootId);
  return centres;
}

function mindMapCentres(rootId, children, depth, orientation, compact) {
  if (compact) return outlineCentres(rootId, children, depth);
  if (orientation === "horizontal" || orientation === "vertical") {
    return treeCentres(rootId, children, depth, orientation === "horizontal");
  }
  return radialCentres(rootId, children, depth);
}

/**
 * @param {{compact?: boolean}} [options] compact = phone-width canvas: an
 *   indented outline with elbow connectors instead of the radial map.
 */
export function layoutMindMap(visualization, { compact = false } = {}) {
  const { nodes, edges, rootNodeId, orientation, savedPositions } = visualization;
  if (nodes.length === 0) return { nodes: [], edges: [] };

  const rootId = nodes.some((n) => n.id === rootNodeId) ? rootNodeId : nodes[0].id;
  const { parent, children, depth } = spanningTree(nodes, edges, rootId);
  const centres = mindMapCentres(rootId, children, depth, orientation, compact);
  // Radial/tree branches run centre to centre; outline branches elbow through the indent gutter.
  const branchHandles = compact
    ? { type: "smoothstep", sourceHandle: "outline-out", targetHandle: "outline-in" }
    : { type: "straight", sourceHandle: "centre-out", targetHandle: "centre-in" };

  const flowNodes = nodes.map((node) => {
    const centre = centres.get(node.id) ?? { x: 0, y: 0 };
    return {
      id: node.id,
      type: "mindMap",
      position: { x: centre.x - MIND_NODE.width / 2, y: centre.y - MIND_NODE.height / 2 },
      data: { label: node.label, isRoot: node.id === rootId, depth: depth.get(node.id) ?? 1 },
    };
  });

  const labelFor = new Map();
  edges.forEach((e) => {
    labelFor.set(`${e.sourceId}>${e.targetId}`, e.label);
    labelFor.set(`${e.targetId}>${e.sourceId}`, e.label);
  });

  const flowEdges = [];
  const drawn = new Set();
  parent.forEach((parentId, id) => {
    if (parentId === null) return;
    drawn.add(`${parentId}>${id}`).add(`${id}>${parentId}`);
    flowEdges.push({
      id: `branch-${parentId}-${id}`,
      source: parentId,
      target: id,
      ...branchHandles,
      // In the outline the indent already says "belongs to"; a label on a short elbow only clutters.
      label: compact ? undefined : labelFor.get(`${parentId}>${id}`) || undefined,
      className: "graph-edge-branch",
    });
  });
  // Relationships beyond the tree ("this also relates to that") stay visible, but quieter.
  edges.forEach((e, index) => {
    // The outline has no room for cross links - they would cut straight through the rows between.
    if (compact) return;
    if (drawn.has(`${e.sourceId}>${e.targetId}`) || e.sourceId === e.targetId) return;
    drawn.add(`${e.sourceId}>${e.targetId}`).add(`${e.targetId}>${e.sourceId}`);
    flowEdges.push({
      id: `link-${index}-${e.sourceId}-${e.targetId}`,
      source: e.sourceId,
      target: e.targetId,
      type: "straight",
      sourceHandle: "centre-out",
      targetHandle: "centre-in",
      label: e.label || undefined,
      className: "graph-edge-link",
    });
  });

  return { nodes: applySavedPositions(flowNodes, savedPositions), edges: flowEdges };
}

// ---- diagram / flowchart ----------------------------------------------------

/** Rank = distance from a start element. Cycles are fine: a node keeps the first rank it is given. */
function rankElements(elements, connections) {
  const ids = new Set(elements.map((el) => el.id));
  const outgoing = new Map(elements.map((el) => [el.id, []]));
  const hasIncoming = new Set();
  connections.forEach((c) => {
    if (!ids.has(c.sourceId) || !ids.has(c.targetId) || c.sourceId === c.targetId) return;
    outgoing.get(c.sourceId).push(c.targetId);
    hasIncoming.add(c.targetId);
  });

  const rank = new Map();
  const order = [];

  function spread(startId) {
    rank.set(startId, 0);
    const queue = [startId];
    while (queue.length > 0) {
      const id = queue.shift();
      order.push(id);
      for (const next of outgoing.get(id)) {
        if (rank.has(next)) continue;
        rank.set(next, rank.get(id) + 1);
        queue.push(next);
      }
    }
  }

  elements.filter((el) => !hasIncoming.has(el.id)).forEach((el) => !rank.has(el.id) && spread(el.id));
  // Whatever is left sits on a cycle with no entry point.
  elements.forEach((el) => !rank.has(el.id) && spread(el.id));

  const rows = [];
  order.forEach((id) => {
    const r = rank.get(id);
    (rows[r] ??= []).push(id);
  });
  return rows;
}

function diagramCentres(rows, compact) {
  const centres = new Map();
  const isSingleChain = rows.every((row) => row.length === 1);
  const perRow = compact ? COMPACT_CHAIN_PER_ROW : CHAIN_PER_ROW;
  const rowGap = compact ? COMPACT_DIAGRAM_ROW_GAP : DIAGRAM_ROW_GAP;

  if (isSingleChain && rows.length >= (compact ? COMPACT_CHAIN_FOLD_FROM : CHAIN_FOLD_FROM)) {
    rows.forEach(([id], index) => {
      const row = Math.floor(index / perRow);
      const step = index % perRow;
      const column = row % 2 === 0 ? step : perRow - 1 - step; // snake back on odd rows
      centres.set(id, { x: column * DIAGRAM_COLUMN_GAP, y: row * rowGap });
    });
    return centres;
  }

  rows.forEach((row, rowIndex) => {
    row.forEach((id, index) => {
      centres.set(id, { x: (index - (row.length - 1) / 2) * DIAGRAM_COLUMN_GAP, y: rowIndex * rowGap });
    });
  });
  return centres;
}

/** @param {{compact?: boolean}} [options] compact = phone-width canvas: chains fold two to a row. */
export function layoutDiagram(visualization, { compact = false } = {}) {
  const { elements, connections, savedPositions } = visualization;
  if (elements.length === 0) return { nodes: [], edges: [] };

  const centres = diagramCentres(rankElements(elements, connections), compact);
  const ids = new Set(elements.map((el) => el.id));

  const flowNodes = elements.map((el) => {
    const centre = centres.get(el.id) ?? { x: 0, y: 0 };
    return {
      id: el.id,
      type: "diagram",
      position: { x: centre.x - DIAGRAM_NODE.width / 2, y: centre.y - DIAGRAM_NODE.height / 2 },
      data: { label: el.label, elementType: el.elementType, accessibilityLabel: el.accessibilityLabel },
    };
  });

  const flowEdges = connections
    .filter((c) => ids.has(c.sourceId) && ids.has(c.targetId) && c.sourceId !== c.targetId)
    .map((c, index) => ({
      id: `conn-${index}-${c.id ?? `${c.sourceId}-${c.targetId}`}`,
      source: c.sourceId,
      target: c.targetId,
      type: "smoothstep",
      label: c.label || undefined,
      className: "graph-edge-flow",
    }));

  return { nodes: applySavedPositions(flowNodes, savedPositions), edges: flowEdges };
}

/**
 * Which sides an edge should leave and enter by, from where the two nodes
 * currently are - recomputed as nodes are dragged, so arrows never loop
 * around a box that was moved to the other side.
 */
export function sidesBetween(sourceNode, targetNode, size = DIAGRAM_NODE) {
  const dx = targetNode.position.x - sourceNode.position.x;
  const dy = targetNode.position.y - sourceNode.position.y;
  // Compare in units of node size so a wide box does not bias towards "sideways".
  if (Math.abs(dx) / size.width > Math.abs(dy) / size.height) {
    return dx >= 0 ? { source: "right", target: "left" } : { source: "left", target: "right" };
  }
  return dy >= 0 ? { source: "bottom", target: "top" } : { source: "top", target: "bottom" };
}

/** { [nodeId]: {x, y} } - the shape a later phase will save and `savedPositions` reads back. */
export function positionsOf(flowNodes) {
  return Object.fromEntries(flowNodes.map((n) => [n.id, { x: Math.round(n.position.x), y: Math.round(n.position.y) }]));
}
