import { useMemo } from "react";
import { MarkerType } from "@xyflow/react";

import DiagramNode from "../graph/DiagramNode.jsx";
import GraphCanvas from "../graph/GraphCanvas.jsx";
import { layoutDiagram, sidesBetween } from "../graph/layout.js";
import { useCompactCanvas } from "../graph/useCompactCanvas.js";
import { useEditableGraph } from "../graph/useEditableGraph.js";

const NODE_TYPES = { diagram: DiagramNode };
/* Matches --muted, the connection stroke colour (SVG markers cannot read CSS variables). */
const ARROW = { type: MarkerType.ArrowClosed, width: 18, height: 18, color: "#735f59" };

/**
 * Consumes the shape produced by domain/visualizationMappers.js's
 * mapDiagram. Rendered with React Flow. This is also what an image falls
 * back to (the AI's flowchart of the same idea, or the rule-based one),
 * so it must read well with nothing but short labels.
 *
 * `onLayoutChange` is the editing hook-up point (see useEditableGraph).
 */
function DiagramCanvas({ visualization, compact, onLayoutChange }) {
  const layout = useMemo(() => layoutDiagram(visualization, { compact }), [visualization, compact]);
  const graph = useEditableGraph(layout, { onLayoutChange });

  // Arrows leave and enter by whichever sides face each other right now, so they follow a dragged box.
  const edges = useMemo(() => {
    const nodesById = new Map(graph.nodes.map((n) => [n.id, n]));
    return layout.edges.map((edge) => {
      const sides = sidesBetween(nodesById.get(edge.source), nodesById.get(edge.target));
      return { ...edge, sourceHandle: `out-${sides.source}`, targetHandle: `in-${sides.target}`, markerEnd: ARROW };
    });
  }, [graph.nodes, layout.edges]);

  const elementTypes = [...new Set(visualization.elements.map((el) => el.elementType))];

  return (
    <div className="diagram-renderer">
      <GraphCanvas
        nodes={graph.nodes}
        edges={edges}
        nodeTypes={NODE_TYPES}
        onNodesChange={graph.onNodesChange}
        isEdited={graph.isEdited}
        onResetLayout={graph.resetLayout}
        ariaLabel="Diagram"
      />

      {elementTypes.length > 1 && (
        <ul className="diagram-legend" aria-hidden="true">
          {elementTypes.map((type) => (
            <li key={type}>
              <span className={`diagram-legend-swatch diagram-node-${type}`} />
              {type.replace("_", " ")}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Keyed by layout mode: crossing the phone breakpoint starts from that mode's own layout. */
function DiagramRenderer(props) {
  const compact = useCompactCanvas();
  return <DiagramCanvas key={compact ? "compact" : "wide"} compact={compact} {...props} />;
}

export default DiagramRenderer;
