import { Suspense, lazy } from "react";

import { ErrorBoundary } from "../../../shared/ErrorBoundary.jsx";
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
  // The boundary also catches a lazily loaded renderer that fails to download, so one
  // broken visual shows a message instead of blanking whatever page it sits on.
  const rendered = (
    <ErrorBoundary fallback={<p className="error-boundary-fallback">Couldn't display this visualization. Try reloading the page.</p>}>
      <Suspense fallback={<div className="visualization-loading" aria-hidden="true" />}>
        <Renderer visualization={visualization} onSave={onSave} />
      </Suspense>
    </ErrorBoundary>
  );
  return assetUrls ? <ArtifactUrlsContext.Provider value={assetUrls}>{rendered}</ArtifactUrlsContext.Provider> : rendered;
}

export default VisualizationRenderer;
