import AnimationRenderer from "../../workspace/renderers/AnimationRenderer.jsx";
import { Pill } from "../../../shared/ui";
import { useInView } from "../../../shared/hooks/useInView.js";

/**
 * One reel slide for a saved animation. Reuses AnimationRenderer as-is
 * (same scene autoplay, same on-demand presigned-URL fetch per scene) -
 * the only thing this adds is reel framing and "replay from scene one
 * each time scrolled back into view" (via keying AnimationRenderer on
 * `enterCount`, which forces a remount).
 */
function SparkAnimationCard({ concept, onOpen }) {
  const { ref, enterCount } = useInView();
  const hasEntered = enterCount > 0;

  return (
    <section className="spark-slide spark-slide-animation" ref={ref}>
      <div className="spark-slide-header">
        {concept.folder && <Pill tone="teal">{concept.folder}</Pill>}
        <Pill tone="coral">ANIMATION</Pill>
      </div>

      <div className="spark-slide-body">
        {hasEntered ? (
          <AnimationRenderer key={enterCount} visualization={concept.visualization} />
        ) : (
          <div className="spark-slide-placeholder" aria-hidden="true" />
        )}
      </div>

      <div className="spark-slide-footer">
        <div>
          <h2>{concept.title}</h2>
          <p>{concept.summary}</p>
        </div>
        <button type="button" className="button button-small" onClick={() => onOpen(concept.id)}>
          Open in Workspace
        </button>
      </div>
    </section>
  );
}

export default SparkAnimationCard;
