import { useSearchParams } from "react-router-dom";
import {
  useConcept,
  useConceptVersions,
  useRegenerateConcept,
  useRelatedConcepts,
  RelatedConceptsPanel,
  VisualizationRenderer,
} from "../features/workspace";
import { Button, OverflowMenu, ProgressDots } from "../shared/ui";
import { Pill } from "../shared/ui/Pill";
import { ErrorBoundary } from "../shared/ErrorBoundary.jsx";
import { visualizationTypeLabel } from "../shared/constants/visualizationTypes.js";

/**
 * Keyed by conceptId at the call site below: React fully remounts this
 * on every conceptId change, so useConcept's state naturally starts
 * fresh without needing to imperatively reset anything.
 *
 * Chrome stays minimal on purpose: title, summary and type badge are the
 * only permanent elements. Delete and Regenerate live behind the ⋯ menu,
 * and version dots appear only when a concept has more than one version.
 * Related concepts (RAG, across folders) sit below the visual, and only
 * when there are any.
 */
function WorkspaceContent({ conceptId }) {
  const { concept, status, errorMessage, remove, reload } = useConcept(conceptId);
  const currentVersion = concept?.currentVersion ?? 1;
  const history = useConceptVersions(conceptId, currentVersion);
  const regeneration = useRegenerateConcept(conceptId);
  const { related } = useRelatedConcepts(conceptId, currentVersion);
  const regenerating = regeneration.status === "loading";

  async function handleDelete() {
    await remove();
    window.location.href = "/library";
  }

  async function handleRegenerate() {
    try {
      await regeneration.regenerate();
      history.showCurrent(); // land on the new version once it's the current one
      reload();
    } catch {
      // regeneration.error carries the message
    }
  }

  if (status === "loading") {
    return (
      <main className="workspace-page page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <p>Loading your concept...</p>
      </main>
    );
  }

  if (status === "error") {
    return (
      <main className="workspace-page page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <h1>Couldn't load this concept</h1>
        <p>{errorMessage}</p>
        <Button to="/library" secondary>
          Back to Library
        </Button>
      </main>
    );
  }

  // Either the current version (the concept's own cached fields) or a historical one.
  const shown = history.historicalVersion ?? concept;
  const dotIndex = history.versions.findIndex((v) => v.version === history.selectedVersion);

  return (
    <main className="workspace-page">
      <div className="workspace-header page-width">
        <div>
          <Pill tone="coral">{visualizationTypeLabel(shown.visualizationType)}</Pill>
          <h1>{shown.title}</h1>
          <p>{shown.summary}</p>
        </div>
        <OverflowMenu
          label="Concept actions"
          items={[
            { label: regenerating ? "Regenerating..." : "Regenerate", onSelect: handleRegenerate, disabled: regenerating },
            { label: "Delete", onSelect: handleDelete, danger: true, disabled: regenerating },
          ]}
        />
      </div>

      {regenerating && (
        <div className="save-toast" role="status">
          Making a new version... your current one stays saved.
        </div>
      )}
      {regeneration.status === "error" && (
        <div className="save-toast save-toast-error" role="alert">
          {regeneration.error} <button type="button" onClick={regeneration.reset}>Dismiss</button>
        </div>
      )}
      {history.error && (
        <div className="save-toast save-toast-error" role="alert">
          {history.error}
        </div>
      )}

      <div className="workspace-canvas-wrapper page-width">
        <ErrorBoundary
          key={history.selectedVersion}
          fallback={<p className="error-boundary-fallback">Couldn't display this visualization.</p>}
        >
          <VisualizationRenderer visualization={shown.visualization} />
        </ErrorBoundary>

        {history.versions.length > 1 && (
          <ProgressDots
            className="workspace-version-dots"
            count={history.versions.length}
            activeIndex={dotIndex}
            onSelect={(i) => history.select(history.versions[i].version)}
            ariaLabel="Versions"
            getKey={(i) => `v${history.versions[i].version}`}
            getLabel={(i) =>
              history.versions[i].version === currentVersion
                ? `Version ${history.versions[i].version} (current)`
                : `Version ${history.versions[i].version}`
            }
          />
        )}
      </div>

      <div className="page-width">
        <RelatedConceptsPanel related={related} />
      </div>
    </main>
  );
}

function WorkspacePage() {
  const [searchParams] = useSearchParams();
  const conceptId = searchParams.get("conceptId");

  if (!conceptId) {
    return (
      <main className="workspace-page page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <h1>No concept selected</h1>
        <p>Go back to Library or create a new one.</p>
        <Button to="/library" secondary>
          Back to Library
        </Button>
      </main>
    );
  }

  return <WorkspaceContent key={conceptId} conceptId={conceptId} />;
}

export default WorkspacePage;
