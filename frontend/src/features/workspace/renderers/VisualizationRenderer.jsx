import { Suspense, lazy } from "react";

import { ArtifactUrlsContext } from "./artifactUrls.js";
import DiagramRenderer from "./DiagramRenderer.jsx";
import ImageRenderer from "./ImageRenderer.jsx";

// React Flow + ELK and the Remotion player are large; they load the first time one of these is shown.
const MindMapRenderer = lazy(() => import("./MindMapRenderer.jsx"));
const AnimationRenderer = lazy(() => import("./AnimationRenderer.jsx"));

const RENDERERS = { mind_map: MindMapRenderer, diagram: DiagramRenderer, animation: AnimationRenderer, image: ImageRenderer };

/**
 * The one place that switches on visualization type. Every renderer
 * consumes only the mapped shape from domain/visualizationMappers.js -
 * never raw backend JSON, never raw LLM output.
 *
 * `assetUrls` (artifact id -> URL) supplies files that don't come from the
 * API; `onSave`, when given, lets an editable renderer save its changes.
 */
function VisualizationRenderer({ visualization, assetUrls, onSave }) {
  const Renderer = RENDERERS[visualization?.type];
  if (!Renderer) {
    return <p className="visualization-error">This visualization type isn't supported yet.</p>;
  }
  const rendered = (
    <Suspense fallback={<div className="visualization-loading" aria-hidden="true" />}>
      <Renderer visualization={visualization} onSave={onSave} />
    </Suspense>
  );
  return assetUrls ? <ArtifactUrlsContext.Provider value={assetUrls}>{rendered}</ArtifactUrlsContext.Provider> : rendered;
}

export default VisualizationRenderer;
