import { Handle, Position } from "@xyflow/react";

import { cx } from "../../../shared/utils/classNames.js";

/** Branch colours cycle by depth so each level of the map reads as one group. */
const DEPTH_CLASS = ["", "mindmap-node-level-1", "mindmap-node-level-2", "mindmap-node-level-3"];

/**
 * Two pairs of connection points, picked per edge by graph/layout.js:
 * - centre-in / centre-out sit at the node's centre (.graph-centre-handle),
 *   so radial branches run centre to centre and tuck under the pills - the
 *   classic mind-map look, and it stays right wherever a node is dragged;
 * - outline-in / outline-out sit on the left edge and under the left end,
 *   for the elbow connectors of the phone-width outline layout.
 */
function MindMapNode({ data, selected }) {
  return (
    <div
      className={cx(
        "mindmap-node",
        data.isRoot ? "mindmap-node-root" : DEPTH_CLASS[Math.min(data.depth, DEPTH_CLASS.length - 1)],
        selected && "mindmap-node-selected"
      )}
    >
      <Handle id="centre-in" type="target" position={Position.Top} className="graph-centre-handle" isConnectable={false} />
      <Handle id="outline-in" type="target" position={Position.Left} className="graph-hidden-handle" isConnectable={false} />
      {data.label}
      <Handle id="centre-out" type="source" position={Position.Top} className="graph-centre-handle" isConnectable={false} />
      <Handle
        id="outline-out"
        type="source"
        position={Position.Bottom}
        className="graph-hidden-handle graph-outline-out-handle"
        isConnectable={false}
      />
    </div>
  );
}

export default MindMapNode;
