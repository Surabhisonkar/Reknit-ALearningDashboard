import { lazy, Suspense } from "react";

import AnimationRenderer from "./AnimationRenderer.jsx";
import ImageRenderer from "./ImageRenderer.jsx";

// The two graph renderers bring in React Flow; loading them on demand keeps
// it out of the bundle for pages that only ever show animations (Spark).
const MindMapRenderer = lazy(() => import("./MindMapRenderer.jsx"));
const DiagramRenderer = lazy(() => import("./DiagramRenderer.jsx"));

const RENDERERS = { mind_map: MindMapRenderer, diagram: DiagramRenderer, animation: AnimationRenderer, image: ImageRenderer };

/**
 * The one place that switches on visualization type. Every renderer
 * consumes only the mapped shape from domain/visualizationMappers.js -
 * never raw backend JSON, never raw LLM output.
 *
 * Renderers cannot tell an AI-made visual from a failsafe one and do
 * not need to: the backend's failsafe produces the same payload types.
 */
function VisualizationRenderer({ visualization }) {
  const Renderer = RENDERERS[visualization?.type];
  if (!Renderer) {
    return <p className="visualization-error">This visualization type isn't supported yet.</p>;
  }
  return (
    <Suspense fallback={<div className="visualization-loading" aria-hidden="true" />}>
      <Renderer visualization={visualization} />
    </Suspense>
  );
}

export default VisualizationRenderer;
