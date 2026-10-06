import { useCallback, useMemo, useState } from "react";

import GraphCanvas from "../graph/GraphCanvas.jsx";
import MindMapNode from "../graph/MindMapNode.jsx";
import { layoutMindMap } from "../graph/layout.js";
import { useCompactCanvas } from "../graph/useCompactCanvas.js";
import { useEditableGraph } from "../graph/useEditableGraph.js";

const NODE_TYPES = { mindMap: MindMapNode };

/**
 * Consumes the shape produced by domain/visualizationMappers.js's
 * mapMindMap - never raw backend JSON. Rendered with React Flow: the
 * same component draws an AI-written map and the rule-based failsafe
 * one, since both arrive as the same payload.
 *
 * `onLayoutChange` is the editing hook-up point (see useEditableGraph).
 */
function MindMapCanvas({ visualization, compact, onLayoutChange }) {
  const { nodes: sourceNodes, rootLabel, citations } = visualization;
  const layout = useMemo(() => layoutMindMap(visualization, { compact }), [visualization, compact]);
  const graph = useEditableGraph(layout, { onLayoutChange });

  const [selectedId, setSelectedId] = useState(null);
  const selectedNode = sourceNodes.find((n) => n.id === selectedId);
  const selectedCitation = citations.find((c) => c.nodeId === selectedId);
  // The citation is the sentence a node came from; repeating it under an identical detail adds nothing.
  const showCitation = selectedCitation && selectedCitation.sourceText !== selectedNode?.detail;

  const handleNodeClick = useCallback((_event, node) => {
    setSelectedId((current) => (current === node.id ? null : node.id));
  }, []);

  return (
    <div className="mindmap-renderer">
      <GraphCanvas
        nodes={graph.nodes}
        edges={layout.edges}
        nodeTypes={NODE_TYPES}
        onNodesChange={graph.onNodesChange}
        onNodeClick={handleNodeClick}
        isEdited={graph.isEdited}
        onResetLayout={graph.resetLayout}
        ariaLabel={`Mind map: ${rootLabel}`}
      />

      {selectedNode && (selectedNode.detail || showCitation) && (
        <aside className="mindmap-inspector" aria-live="polite">
          <h3>{selectedNode.label}</h3>
          {selectedNode.detail && <p>{selectedNode.detail}</p>}
          {showCitation && <small className="mindmap-citation">Source: {selectedCitation.sourceText}</small>}
        </aside>
      )}
    </div>
  );
}

/** Keyed by layout mode: crossing the phone breakpoint starts from that mode's own layout. */
function MindMapRenderer(props) {
  const compact = useCompactCanvas();
  return <MindMapCanvas key={compact ? "compact" : "wide"} compact={compact} {...props} />;
}

export default MindMapRenderer;
