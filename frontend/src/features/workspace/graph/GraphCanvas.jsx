import { useCallback } from "react";
import { Background, Controls, ReactFlow, ReactFlowProvider, useReactFlow } from "@xyflow/react";

import "@xyflow/react/dist/style.css";

const FIT_VIEW = { padding: 0.15, maxZoom: 1.15 };

function Canvas({ nodes, edges, nodeTypes, onNodesChange, onNodeClick, isEdited, onResetLayout, ariaLabel }) {
  const { fitView } = useReactFlow();

  const handleReset = useCallback(() => {
    onResetLayout();
    // Wait for the reset positions to render before fitting to them.
    requestAnimationFrame(() => fitView(FIT_VIEW));
  }, [fitView, onResetLayout]);

  return (
    <div className="graph-canvas" role="group" aria-label={ariaLabel}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={onNodesChange}
        onNodeClick={onNodeClick}
        fitView
        fitViewOptions={FIT_VIEW}
        minZoom={0.2}
        maxZoom={2}
        nodesConnectable={false}
        edgesFocusable={false}
        deleteKeyCode={null}
        // The canvas sits inside a scrolling page (and inside the Capture
        // draft): the wheel must keep scrolling the page, never get trapped
        // zooming the map. Zoom is on the buttons and on pinch.
        zoomOnScroll={false}
        preventScrolling={false}
      >
        <Background gap={24} size={1} />
        <Controls showInteractive={false} position="bottom-left" orientation="horizontal" />
      </ReactFlow>

      {isEdited && (
        <button type="button" className="graph-reset" onClick={handleReset}>
          Reset layout
        </button>
      )}
    </div>
  );
}

/**
 * The one React Flow canvas both graph renderers share: drag to
 * rearrange, pinch or buttons to zoom, "Reset layout" once something has
 * been moved. Renderers supply nodes, edges and their own node types.
 */
function GraphCanvas(props) {
  return (
    <ReactFlowProvider>
      <Canvas {...props} />
    </ReactFlowProvider>
  );
}

export default GraphCanvas;
