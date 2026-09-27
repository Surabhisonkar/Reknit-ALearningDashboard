import { Button, Pill } from "../../../shared/ui";

/**
 * The inline review step on Capture: shows an unsaved draft and its three
 * choices. Presentational only - the visualization itself is passed in as
 * `children`, so this feature never depends on the renderer feature.
 */
function DraftPreview({ draft, typeLabel, saving, regenerating, onSave, onRegenerate, onDiscard, children }) {
  const busy = saving || regenerating;

  return (
    <section className="draft-preview" aria-labelledby="draft-preview-title" aria-busy={regenerating}>
      <div className="draft-preview-header">
        <div className="draft-preview-badges">
          <Pill tone="coral">{typeLabel}</Pill>
          <Pill tone="neutral">Not saved yet</Pill>
        </div>
        <h2 id="draft-preview-title">{draft.title}</h2>
        <p>{draft.summary}</p>
      </div>

      <div className={regenerating ? "draft-preview-canvas is-regenerating" : "draft-preview-canvas"}>{children}</div>

      <div className="draft-actions">
        <Button secondary onClick={onDiscard} disabled={busy}>
          Discard
        </Button>
        <Button secondary onClick={onRegenerate} disabled={busy}>
          {regenerating ? "Trying again..." : "Regenerate"}
        </Button>
        <Button onClick={onSave} disabled={busy}>
          {saving ? "Saving..." : "Save to library"}
        </Button>
      </div>
    </section>
  );
}

export default DraftPreview;
