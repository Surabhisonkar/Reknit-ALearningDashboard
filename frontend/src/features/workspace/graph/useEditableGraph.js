import { useCallback, useMemo, useRef, useState } from "react";
import { applyNodeChanges } from "@xyflow/react";

import { positionsOf } from "./layout.js";

/**
 * Owns the editable state of one graph canvas: where the nodes currently
 * are, whether the person has moved any, and how to put them back.
 *
 * This is the foundation for editing (roadmap phase "Editable visuals"):
 * - `onLayoutChange(positions)` fires once per finished drag with every
 *   node's position, and with `null` on reset. Nothing listens yet; the
 *   next phase passes a callback that saves them.
 * - Saved positions come back through the mapped visualization's
 *   `savedPositions` and are applied by graph/layout.js.
 * So persisting edits later means wiring an endpoint to these two ends,
 * without touching the renderers.
 *
 * The caller must remount (React `key`) when it shows a different
 * visualization - both pages already key the renderer by draft/version.
 */
export function useEditableGraph(layout, { onLayoutChange } = {}) {
  const [nodes, setNodes] = useState(layout.nodes);
  // Mirrors `nodes` so a burst of changes in one frame builds on each other, not on a stale render.
  const latestNodes = useRef(layout.nodes);
  const [isEdited, setIsEdited] = useState(false);

  const onNodesChange = useCallback(
    (changes) => {
      const next = applyNodeChanges(changes, latestNodes.current);
      latestNodes.current = next;
      setNodes(next);

      const dragFinished = changes.some((change) => change.type === "position" && change.dragging === false);
      if (dragFinished) {
        setIsEdited(true);
        onLayoutChange?.(positionsOf(next));
      }
    },
    [onLayoutChange]
  );

  const resetLayout = useCallback(() => {
    latestNodes.current = layout.nodes;
    setNodes(layout.nodes);
    setIsEdited(false);
    onLayoutChange?.(null);
  }, [layout.nodes, onLayoutChange]);

  return useMemo(
    () => ({ nodes, onNodesChange, isEdited, resetLayout }),
    [nodes, onNodesChange, isEdited, resetLayout]
  );
}
