import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { useExplainFlow, ExplanationRenderer } from "../features/generate";
import { useVisualizeFlow, useSaveDraft, DraftPreview, DuplicateTitleModal } from "../features/capture";
import { VisualizationRenderer } from "../features/workspace";
import { explanationToPlainText } from "../domain";
import { Button } from "../shared/ui";
import { Icon } from "../shared/ui/Icon";
import { Pill } from "../shared/ui/Pill";
import { ErrorBoundary } from "../shared/ErrorBoundary.jsx";
import { visualizationTypeLabel } from "../shared/constants/visualizationTypes.js";

/**
 * Capture: topic/notes -> (optional) inline explanation -> inline
 * visualization *draft* -> Save / Regenerate / Discard. Nothing is written
 * to the library until Save; the duplicate-title check runs as part of
 * Save, before anything is stored.
 */
function CapturePage() {
  const navigate = useNavigate();

  // Spark's "Make a visual" arrives with the chat pre-filled; a normal visit has no state.
  const prefill = useLocation().state?.prefill;
  const [title, setTitle] = useState(prefill?.title ?? "");
  const [description, setDescription] = useState(prefill?.description ?? "");

  const explainFlow = useExplainFlow();
  const visualizeFlow = useVisualizeFlow();
  const saveDraft = useSaveDraft();

  // Set only when Save hits an existing title - holds what the modal shows.
  const [duplicateTitle, setDuplicateTitle] = useState(null);

  const hasTopic = title.trim().length > 0;
  const hasOwnNotes = description.trim().length > 0;
  const hasExplanation = explainFlow.status === "ready" && explainFlow.explanation !== null;
  const canVisualize = hasExplanation || hasOwnNotes;
  const { draft } = visualizeFlow;
  const visualizing = visualizeFlow.status === "loading";

  async function handleVisualize() {
    const conceptText =
      hasExplanation && explainFlow.useAiExplanation
        ? explanationToPlainText(explainFlow.explanation)
        : title
          ? `${title}\n\n${description}`
          : description;

    saveDraft.clearError();
    try {
      await visualizeFlow.visualize(conceptText, { sourceExplainJobId: explainFlow.jobId });
    } catch {
      // visualizeFlow.error already carries the message - nothing else to do here.
    }
  }

  async function handleRegenerateDraft() {
    saveDraft.clearError();
    try {
      await visualizeFlow.regenerate();
    } catch {
      // surfaced via visualizeFlow.error; the previous draft stays on screen
    }
  }

  function handleDiscard() {
    saveDraft.clearError();
    setDuplicateTitle(null);
    visualizeFlow.discard();
  }

  /** One path for every save attempt - first try and each duplicate resolution. */
  async function attemptSave(options) {
    const result = await saveDraft.save(draft.jobId, options);
    if (result.outcome === "saved") {
      setDuplicateTitle(null);
      navigate(`/workspace?conceptId=${result.concept.id}`);
    } else if (result.outcome === "duplicate") {
      setDuplicateTitle(result.title);
    } else {
      setDuplicateTitle(null);
    }
  }

  return (
    <main className="capture-page page-width">
      <div className="capture-heading">
        <div>
          <Pill tone="coral">Reknit / CREATE</Pill>
          <h1>What are we untangling today?</h1>
          <p>Give it a topic - notes are optional, Reknit can explain it from scratch.</p>
        </div>
      </div>

      <form className="capture-form" onSubmit={(event) => event.preventDefault()}>
        <label>
          Topic
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="e.g. The Zeigarnik effect"
          />
        </label>
        <label>
          Your notes <span className="field-optional">(optional)</span>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Add your own rough notes if you have them - or leave this blank and let the AI explain it for you."
          />
        </label>

        <div className="capture-footer">
          <span>Just a topic is enough. Reknit fills in the rest.</span>
          <span>{description.length} characters</span>
          <div className="capture-actions">
            <Button
              type="button"
              onClick={() => explainFlow.explain(title, description)}
              disabled={explainFlow.status === "loading" || !hasTopic}
            >
              {explainFlow.status === "loading" ? "Thinking..." : "Explain this"} <Icon>{"->"}</Icon>
            </Button>
          </div>
        </div>
      </form>

      {explainFlow.status === "error" && (
        <div className="save-toast save-toast-error" role="alert">
          {explainFlow.error} <button type="button" onClick={explainFlow.reset}>Dismiss</button>
        </div>
      )}

      {/* Renders right below the form, in place - never a separate page, so it's easy to read
          alongside your own notes and copy from directly if you want to. */}
      {hasExplanation && (
        <section className="inline-explanation">
          <Pill tone="teal">{explainFlow.explanation.suggestedTitle}</Pill>
          <ErrorBoundary fallback={<p className="error-boundary-fallback">Couldn't display this explanation. Try generating it again.</p>}>
            <ExplanationRenderer explanation={explainFlow.explanation} />
          </ErrorBoundary>

          {hasOwnNotes && (
            <div className="explanation-choice" role="radiogroup" aria-label="Which text to visualize">
              <label className="explanation-option">
                <input
                  type="radio"
                  name="explanationChoice"
                  checked={explainFlow.useAiExplanation}
                  onChange={() => explainFlow.setUseAiExplanation(true)}
                />
                Visualize the AI's explanation
              </label>
              <label className="explanation-option">
                <input
                  type="radio"
                  name="explanationChoice"
                  checked={!explainFlow.useAiExplanation}
                  onChange={() => explainFlow.setUseAiExplanation(false)}
                />
                Visualize my own notes instead
              </label>
            </div>
          )}
        </section>
      )}

      {canVisualize && (
        <div className="generate-actions">
          <Button onClick={handleVisualize} disabled={visualizing || saveDraft.saving}>
            {visualizing && !draft ? "Visualizing..." : draft ? "Visualize again from these notes" : "Visualize this"}{" "}
            <Icon>{"->"}</Icon>
          </Button>
        </div>
      )}

      {visualizeFlow.status === "error" && (
        <div className="save-toast save-toast-error" role="alert">
          {visualizeFlow.error} <button type="button" onClick={visualizeFlow.reset}>Dismiss</button>
        </div>
      )}

      {draft && (
        <DraftPreview
          draft={draft}
          typeLabel={visualizationTypeLabel(draft.visualizationType)}
          saving={saveDraft.saving}
          regenerating={visualizing}
          onSave={() => attemptSave()}
          onRegenerate={handleRegenerateDraft}
          onDiscard={handleDiscard}
        >
          <ErrorBoundary
            key={draft.jobId}
            fallback={<p className="error-boundary-fallback">Couldn't display this visualization. Try regenerating it.</p>}
          >
            <VisualizationRenderer visualization={draft.visualization} />
          </ErrorBoundary>
        </DraftPreview>
      )}

      {saveDraft.error && (
        <div className="save-toast save-toast-error" role="alert">
          {saveDraft.error} <button type="button" onClick={saveDraft.clearError}>Dismiss</button>
        </div>
      )}

      {duplicateTitle && (
        <DuplicateTitleModal
          key={duplicateTitle}
          title={duplicateTitle}
          busy={saveDraft.saving}
          onRename={(newTitle) => attemptSave({ title: newTitle })}
          onReplace={() => attemptSave({ onDuplicate: "REPLACE" })}
          onKeepBoth={() => attemptSave({ onDuplicate: "KEEP_BOTH" })}
          onClose={() => setDuplicateTitle(null)}
        />
      )}
    </main>
  );
}

export default CapturePage;
