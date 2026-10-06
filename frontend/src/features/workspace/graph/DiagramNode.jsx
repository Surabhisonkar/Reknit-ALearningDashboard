import { Handle, Position } from "@xyflow/react";

import { cx } from "../../../shared/utils/classNames.js";

const DIAGRAM_SIDES = ["top", "right", "bottom", "left"];

const POSITION_BY_SIDE = { top: Position.Top, right: Position.Right, bottom: Position.Bottom, left: Position.Left };

/**
 * One flowchart box. Shape and colour both come from `elementType`
 * (see .diagram-node-* in index.css). A source and a target handle on
 * every side lets DiagramRenderer route each arrow by the shortest way.
 */
function DiagramNode({ data, selected }) {
  return (
    <div
      className={cx("diagram-node", `diagram-node-${data.elementType}`, selected && "diagram-node-selected")}
      role="img"
      aria-label={data.accessibilityLabel}
    >
      {DIAGRAM_SIDES.map((side) => (
        <Handle
          key={`in-${side}`}
          id={`in-${side}`}
          type="target"
          position={POSITION_BY_SIDE[side]}
          className="graph-hidden-handle"
          isConnectable={false}
        />
      ))}
      <span className="diagram-node-label">{data.label}</span>
      {DIAGRAM_SIDES.map((side) => (
        <Handle
          key={`out-${side}`}
          id={`out-${side}`}
          type="source"
          position={POSITION_BY_SIDE[side]}
          className="graph-hidden-handle"
          isConnectable={false}
        />
      ))}
    </div>
  );
}

export default DiagramNode;
