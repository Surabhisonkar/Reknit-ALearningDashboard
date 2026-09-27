import AnimationRenderer from "./AnimationRenderer.jsx";
import DiagramRenderer from "./DiagramRenderer.jsx";
import ImageRenderer from "./ImageRenderer.jsx";
import MindMapRenderer from "./MindMapRenderer.jsx";

const RENDERERS = { mind_map: MindMapRenderer, diagram: DiagramRenderer, animation: AnimationRenderer, image: ImageRenderer };

/**
 * The one place that switches on visualization type. Every renderer
 * consumes only the mapped shape from domain/visualizationMappers.js -
 * never raw backend JSON, never raw LLM output.
 */
function VisualizationRenderer({ visualization }) {
  const Renderer = RENDERERS[visualization?.type];
  if (!Renderer) {
    return <p className="visualization-error">This visualization type isn't supported yet.</p>;
  }
  return <Renderer visualization={visualization} />;
}

export default VisualizationRenderer;
