import { useState } from "react";

const RADIUS = 200;
const CENTER = 260;

function positionsFor(nodes, rootNodeId) {
  const others = nodes.filter((n) => n.id !== rootNodeId);
  const angleStep = (2 * Math.PI) / Math.max(others.length, 1);
  const positions = new Map();
  if (rootNodeId) positions.set(rootNodeId, { x: CENTER, y: CENTER });
  others.forEach((node, i) => {
    const angle = i * angleStep - Math.PI / 2;
    positions.set(node.id, {
      x: CENTER + RADIUS * Math.cos(angle),
      y: CENTER + RADIUS * Math.sin(angle),
    });
  });
  return positions;
}

/** Consumes the shape produced by domain/visualizationMappers.js's mapMindMap - never raw backend JSON. */
function MindMapRenderer({ visualization }) {
  const { nodes, edges, rootLabel, rootNodeId, citations } = visualization;
  const positions = positionsFor(nodes, rootNodeId);
  const [selectedId, setSelectedId] = useState(null);
  const selectedNode = nodes.find((n) => n.id === selectedId);
  const selectedCitation = citations.find((c) => c.nodeId === selectedId);

  return (
    <div className="mindmap-renderer">
      <svg viewBox="0 0 520 520" className="mindmap-svg" role="img" aria-label={`Mind map: ${rootLabel}`}>
        {edges.map((edge) => {
          const from = positions.get(edge.sourceId);
          const to = positions.get(edge.targetId);
          if (!from || !to) return null;
          return (
            <line
              key={`${edge.sourceId}-${edge.targetId}`}
              x1={from.x} y1={from.y} x2={to.x} y2={to.y}
              className="mindmap-edge"
            />
          );
        })}
        {rootNodeId &&
          nodes
            .filter((n) => n.id !== rootNodeId)
            .map((node) => {
              const from = positions.get(rootNodeId);
              const to = positions.get(node.id);
              return (
                <line
                  key={`root-${node.id}`}
                  x1={from.x} y1={from.y} x2={to.x} y2={to.y}
                  className="mindmap-branch"
                />
              );
            })}
      </svg>

      <div className="mindmap-nodes">
        {nodes.map((node) => {
          const pos = positions.get(node.id);
          const isRoot = node.id === rootNodeId;
          return (
            <button
              key={node.id}
              type="button"
              className={`mindmap-node ${isRoot ? "mindmap-node-root" : ""} ${selectedId === node.id ? "mindmap-node-selected" : ""}`}
              style={{ left: pos?.x ?? 0, top: pos?.y ?? 0 }}
              onClick={() => setSelectedId(node.id === selectedId ? null : node.id)}
            >
              {node.label}
            </button>
          );
        })}
      </div>

      {selectedNode && (
        <aside className="mindmap-inspector">
          <h3>{selectedNode.label}</h3>
          {selectedNode.detail && <p>{selectedNode.detail}</p>}
          {selectedCitation && <small className="mindmap-citation">Source: {selectedCitation.sourceText}</small>}
        </aside>
      )}
    </div>
  );
}

export default MindMapRenderer;
