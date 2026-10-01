import { VisualizationRenderer } from "../../workspace";
import { revealEffectClass } from "../reveal/revealEffects.js";

/**
 * The revealed visual, entering with its type's effect (see reveal/revealEffects.js).
 * It scrolls by finger and ignores feed swipes (user decision): swipe on the
 * card's header or footer to move on.
 */
export default function RevealStage({ visualization }) {
  return (
    <div className={`reveal-stage ${revealEffectClass(visualization?.type)}`} data-gesture-ignore>
      <VisualizationRenderer visualization={visualization} />
    </div>
  );
}
