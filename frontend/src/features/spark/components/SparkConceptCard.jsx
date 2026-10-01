import { folderColorClass } from "../../../shared/constants/folderColors.js";
import { visualizationTypeLabel } from "../../../shared/constants/visualizationTypes.js";
import { useNarration } from "../../../shared/narration";
import { SvgIcon } from "../../../shared/ui";
import { cx } from "../../../shared/utils/classNames.js";
import DeepDiveMenu from "./DeepDiveMenu.jsx";
import NarrationButton from "./NarrationButton.jsx";
import RevealStage from "./RevealStage.jsx";

/**
 * One concept in the feed. Teaser first (title only - a small recall moment),
 * then tap to reveal the visual; the narrator reads the title and summary
 * unless muted. `concept.visualization` may arrive late (related cards load
 * it on reveal), so the revealed state shows a loader until it does.
 */
export default function SparkConceptCard({ concept, revealed, loading, onReveal, onRelated, onAsk, relatedLabel }) {
  const narration = useNarration();

  function reveal() {
    onReveal();
    if (!narration.muted) narration.speak(`${concept.title}. ${concept.summary ?? ""}`);
  }

  return (
    <article className={cx("spark-card", folderColorClass(concept.folderColor), revealed && "is-revealed")}>
      <span className="spark-card-band" aria-hidden="true" />
      <header className="spark-card-meta">
        <span className="spark-card-type">{visualizationTypeLabel(concept.visualizationType)}</span>
        {concept.folderId && <span className="spark-card-folder">{concept.folder}</span>}
      </header>

      {!revealed ? (
        <button type="button" className="spark-teaser" onClick={reveal}>
          <h2>{concept.title}</h2>
          <span className="spark-teaser-hint">
            <SvgIcon name="sparkle" size={18} /> Tap to reveal
          </span>
        </button>
      ) : (
        <>
          <div className="spark-card-visual">
            {loading || !concept.visualization ? (
              <div className="spark-card-loading">Loading the visual…</div>
            ) : (
              <RevealStage visualization={concept.visualization} />
            )}
          </div>
          <footer className="spark-card-footer">
            <h2>{concept.title}</h2>
            <div className="spark-card-actions">
              <NarrationButton narration={narration} />
              {onRelated && (
                <button type="button" className="spark-icon-button" aria-label={relatedLabel} title={relatedLabel} onClick={onRelated}>
                  <SvgIcon name="layers" />
                </button>
              )}
              <DeepDiveMenu concept={concept} onAsk={onAsk} />
            </div>
          </footer>
        </>
      )}
    </article>
  );
}
