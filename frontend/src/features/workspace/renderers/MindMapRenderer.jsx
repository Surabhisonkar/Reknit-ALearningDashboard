import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Background,
  Controls,
  Handle,
  Position,
  ReactFlow,
  ReactFlowProvider,
  useNodesState,
  useReactFlow,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import {
  NODE_HEIGHT,
  ROOT_HEIGHT,
  childrenIndex,
  isInSubtree,
  layoutTree,
  nodeWidth,
  removeSubtree,
  visibleNodes,
} from "./mindmap/mindMapTree.js";

/** One branch: double-click to rename, the round tab on its right edge expands or collapses its children. */
const BranchNode = memo(function BranchNode({ id, data }) {
  const [draft, setDraft] = useState(data.label);

  function commit() {
    const label = draft.trim();
    data.onRename(id, label || data.label);
  }

  return (
    <div
      className={`mm-node ${data.isRoot ? "mm-node-root" : `mm-node-depth-${Math.min(data.depth, 3)}`} ${data.selected ? "mm-node-selected" : ""}`}
      style={{ width: data.width, minHeight: data.isRoot ? ROOT_HEIGHT : NODE_HEIGHT }}
      onDoubleClick={() => {
        setDraft(data.label);
        data.onStartEdit(id);
      }}
    >
      <Handle type="target" position={Position.Left} className="mm-handle" isConnectable={false} />
      {data.editing ? (
        <input
          className="mm-node-input nodrag"
          value={draft}
          autoFocus
          maxLength={80}
          aria-label="Branch label"
          onChange={(event) => setDraft(event.target.value)}
          onBlur={commit}
          onKeyDown={(event) => {
            if (event.key === "Enter") commit();
            if (event.key === "Escape") data.onRename(id, data.label);
          }}
        />
      ) : (
        <span className="mm-node-label">{data.label}</span>
      )}
      {data.childCount > 0 && (
        <button
          type="button"
          className="mm-toggle nodrag"
          aria-label={data.collapsed ? `Expand ${data.label}` : `Collapse ${data.label}`}
          aria-expanded={!data.collapsed}
          onClick={(event) => {
            event.stopPropagation();
            data.onToggle(id);
          }}
        >
          {data.collapsed ? data.childCount : "-"}
        </button>
      )}
      <Handle type="source" position={Position.Right} className="mm-handle" isConnectable={false} />
    </div>
  );
});

const NODE_TYPES = { branch: BranchNode };

let nextNodeId = 1;

/** The edited tree in the API's mind map shape (version 2), ready to save. */
function toPayload(visualization, tree) {
  const ids = new Set(tree.map((n) => n.id));
  const root = tree.find((n) => n.id === visualization.rootNodeId);
  return {
    type: "mind_map",
    version: 2,
    rootLabel: root?.label ?? visualization.rootLabel,
    nodes: tree.map(({ id, label, detail, parentId }) => ({ id, label, detail: detail ?? "", parentId: parentId ?? null })),
    edges: visualization.crossLinks
      .filter((l) => ids.has(l.sourceId) && ids.has(l.targetId))
      .map((l) => ({ sourceId: l.sourceId, targetId: l.targetId, relationshipLabel: l.label })),
    layoutHints: { orientation: "horizontal", rootNodeId: visualization.rootNodeId },
    citations: visualization.citations.filter((c) => ids.has(c.nodeId)).map((c) => ({ nodeId: c.nodeId, sourceText: c.sourceText })),
  };
}

function MindMapFlow({ visualization, onSave }) {
  const { rootNodeId, citations, crossLinks } = visualization;
  const [tree, setTree] = useState(visualization.nodes);
  const [collapsed, setCollapsed] = useState(() => new Set());
  const [selectedId, setSelectedId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [layout, setLayout] = useState({ version: 0, positions: null });
  const [layoutRequest, setLayoutRequest] = useState(0);
  const [save, setSave] = useState({ status: "idle", error: null }); // idle | saving | error
  const [rfNodes, setRfNodes, onNodesChange] = useNodesState([]);
  const { fitView, getIntersectingNodes } = useReactFlow();
  const layoutRun = useRef(0);
  const appliedLayout = useRef(0);

  const children = useMemo(() => childrenIndex(tree), [tree]);
  const shown = useMemo(() => visibleNodes(tree, rootNodeId, collapsed), [tree, rootNodeId, collapsed]);
  const depthOf = useMemo(() => {
    const depth = new Map([[rootNodeId, 0]]);
    shown.forEach((n) => n.parentId && depth.set(n.id, (depth.get(n.parentId) ?? 0) + 1));
    return depth;
  }, [shown, rootNodeId]);

  // Re-run ELK whenever what's shown (or a label's width) changes, or "Tidy layout" is pressed.
  const layoutKey = shown.map((n) => `${n.id}:${n.parentId}:${nodeWidth(n, n.id === rootNodeId)}`).join("|");
  useEffect(() => {
    const run = ++layoutRun.current;
    layoutTree(shown, rootNodeId).then((laidOut) => {
      if (run !== layoutRun.current) return;
      setLayout({ version: run, positions: laidOut });
      requestAnimationFrame(() => fitView({ padding: 0.12, duration: 250 }));
    });
    // `shown` is captured through layoutKey on purpose: label edits that keep the width don't move anything.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [layoutKey, layoutRequest, rootNodeId, fitView]);

  const onToggle = useCallback((id) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);
  const onRename = useCallback((id, label) => {
    setTree((prev) => prev.map((n) => (n.id === id ? { ...n, label } : n)));
    setEditingId(null);
  }, []);
  const onStartEdit = useCallback((id) => setEditingId(id), []);

  // A fresh layout places every node; any other update keeps wherever the user dragged them.
  useEffect(() => {
    if (!layout.positions) return;
    const fresh = layout.version !== appliedLayout.current;
    appliedLayout.current = layout.version;
    setRfNodes((prev) => {
      const current = new Map(prev.map((n) => [n.id, n.position]));
      return shown.map((n) => ({
        id: n.id,
        type: "branch",
        position: (fresh ? null : current.get(n.id)) ?? layout.positions.get(n.id) ?? { x: 0, y: 0 },
        data: {
          label: n.label,
          isRoot: n.id === rootNodeId,
          depth: depthOf.get(n.id) ?? 1,
          width: nodeWidth(n, n.id === rootNodeId),
          childCount: children.get(n.id)?.length ?? 0,
          collapsed: collapsed.has(n.id),
          selected: n.id === selectedId,
          editing: n.id === editingId,
          onToggle,
          onRename,
          onStartEdit,
        },
      }));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [layout, tree, collapsed, selectedId, editingId]);

  const edges = useMemo(() => {
    const ids = new Set(shown.map((n) => n.id));
    const treeEdges = shown
      .filter((n) => n.parentId && ids.has(n.parentId))
      .map((n) => ({ id: `t-${n.parentId}-${n.id}`, source: n.parentId, target: n.id, className: "mm-edge" }));
    const links = crossLinks
      .filter((l) => ids.has(l.sourceId) && ids.has(l.targetId))
      .map((l, i) => ({
        id: `x-${i}`,
        source: l.sourceId,
        target: l.targetId,
        label: l.label || undefined,
        className: "mm-edge-link",
        animated: true,
      }));
    return [...treeEdges, ...links];
  }, [shown, crossLinks]);

  /** Dropping a branch onto another node moves it (and its subtree) under that node. */
  function onNodeDragStop(_event, node) {
    if (node.id === rootNodeId) return;
    const target = getIntersectingNodes(node).find((n) => n.id !== node.id && !isInSubtree(tree, node.id, n.id));
    if (!target) return;
    setTree((prev) => prev.map((n) => (n.id === node.id ? { ...n, parentId: target.id } : n)));
    setCollapsed((prev) => {
      const next = new Set(prev);
      next.delete(target.id);
      return next;
    });
  }

  function addBranch() {
    const parentId = selectedId ?? rootNodeId;
    const id = `new-${nextNodeId++}`;
    setTree((prev) => [...prev, { id, label: "New branch", detail: "", parentId }]);
    setCollapsed((prev) => {
      const next = new Set(prev);
      next.delete(parentId);
      return next;
    });
    setSelectedId(id);
    setEditingId(id);
  }

  function deleteSelected() {
    if (!selectedId || selectedId === rootNodeId) return;
    setTree((prev) => removeSubtree(prev, selectedId));
    setSelectedId(null);
  }

  const selectedNode = tree.find((n) => n.id === selectedId);
  const selectedCitation = citations.find((c) => c.nodeId === selectedId);
  const changed = tree !== visualization.nodes;

  async function handleSave() {
    setSave({ status: "saving", error: null });
    try {
      await onSave(toPayload(visualization, tree));
      // Success remounts this view with the saved version, so no state to reset here.
    } catch (error) {
      setSave({ status: "error", error: error.message || "Couldn't save your changes." });
    }
  }

  return (
    <div className="mindmap-renderer">
      <div className="mm-toolbar" role="toolbar" aria-label="Mind map tools">
        <button type="button" className="viz-chip" onClick={addBranch}>
          Add branch
        </button>
        <button type="button" className="viz-chip" onClick={deleteSelected} disabled={!selectedId || selectedId === rootNodeId}>
          Delete
        </button>
        <button type="button" className="viz-chip" onClick={() => setCollapsed(new Set())}>
          Expand all
        </button>
        <button
          type="button"
          className="viz-chip"
          onClick={() => setCollapsed(new Set([...children.entries()].filter(([id, kids]) => kids.length && id !== rootNodeId).map(([id]) => id)))}
        >
          Collapse all
        </button>
        <button type="button" className="viz-chip" onClick={() => setLayoutRequest((n) => n + 1)}>
          Tidy layout
        </button>
        {changed && (
          <button
            type="button"
            className="viz-chip"
            onClick={() => {
              setTree(visualization.nodes);
              setCollapsed(new Set());
              setSelectedId(null);
              setSave({ status: "idle", error: null });
            }}
          >
            Reset
          </button>
        )}
        {onSave && changed && (
          <button type="button" className="viz-chip viz-chip-primary" onClick={handleSave} disabled={save.status === "saving"}>
            {save.status === "saving" ? "Saving..." : "Save changes"}
          </button>
        )}
        <span className="mm-hint">Double-click to rename. Drag a branch onto another to move it.</span>
      </div>
      {save.status === "error" && (
        <p className="mm-save-error" role="alert">
          {save.error}
        </p>
      )}

      <div className="mm-canvas" aria-label={`Mind map: ${visualization.rootLabel}`}>
        <ReactFlow
          nodes={rfNodes}
          edges={edges}
          nodeTypes={NODE_TYPES}
          onNodesChange={onNodesChange}
          onNodeClick={(_e, node) => setSelectedId(node.id)}
          onPaneClick={() => setSelectedId(null)}
          onNodeDragStop={onNodeDragStop}
          nodesConnectable={false}
          zoomOnScroll={false}
          preventScrolling={false}
          zoomOnDoubleClick={false}
          minZoom={0.3}
          maxZoom={1.6}
          proOptions={{ hideAttribution: true }}
        >
          <Background gap={22} size={1} color="var(--line)" />
          <Controls showInteractive={false} position="bottom-right" />
        </ReactFlow>
      </div>

      {selectedNode && (selectedNode.detail || selectedCitation) && (
        <aside className="mindmap-inspector">
          <h3>{selectedNode.label}</h3>
          {selectedNode.detail && <p>{selectedNode.detail}</p>}
          {selectedCitation && <small className="mindmap-citation">Source: {selectedCitation.sourceText}</small>}
        </aside>
      )}
    </div>
  );
}

/**
 * Consumes the shape produced by domain/visualizationMappers.js's mapMindMap
 * (always a tree, whatever version was stored). React Flow draws it, ELK.js
 * arranges it. Expanding, renaming, adding, deleting and moving branches
 * change this view; when `onSave` is given, "Save changes" stores the edited
 * tree as the concept's next version.
 */
function MindMapRenderer({ visualization, onSave }) {
  return (
    <ReactFlowProvider>
      <MindMapFlow visualization={visualization} onSave={onSave} />
    </ReactFlowProvider>
  );
}

export default MindMapRenderer;
