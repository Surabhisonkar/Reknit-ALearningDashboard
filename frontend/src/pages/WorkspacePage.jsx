import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  ConceptSidebar,
  useConcept,
  useConceptVersions,
  useRegenerateConcept,
  useRelatedConcepts,
  RelatedConceptsPanel,
  useConceptNotes,
  ConceptNotesPanel,
  SourceNotesPanel,
  VisualizationRenderer,
} from "../features/workspace";
import { Button, OverflowMenu, ProgressDots, SvgIcon } from "../shared/ui";
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
  const { concept, status, errorMessage, remove, reload, saveVisualization } = useConcept(conceptId);
  const currentVersion = concept?.currentVersion ?? 1;
  const history = useConceptVersions(conceptId, currentVersion);
  const regeneration = useRegenerateConcept(conceptId);
  const { related } = useRelatedConcepts(conceptId, currentVersion);
  const { notes, remove: removeNote } = useConceptNotes(conceptId);
  const regenerating = regeneration.status === "loading";
  const navigate = useNavigate();

  async function handleDelete() {
    await remove();
    navigate("/library");
  }

  async function handleSaveVisualization(payload) {
    await saveVisualization(payload);
    history.showCurrent(); // the saved edit is the new current version
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
          <VisualizationRenderer
            visualization={shown.visualization}
            // Only the current version can be edited and saved; an older one is view-only history.
            onSave={history.historicalVersion ? undefined : handleSaveVisualization}
          />
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
        <ConceptNotesPanel notes={notes} onDelete={removeNote} />
        <SourceNotesPanel conceptId={conceptId} version={history.selectedVersion ?? currentVersion} />
      </div>
    </main>
  );
}

/**
 * The library sidebar sits outside the keyed content, so it keeps its open
 * folders and search while you move between concepts. On narrow screens it
 * slides in from the left behind a "Concepts" button.
 */
function WorkspacePage() {
  const [searchParams] = useSearchParams();
  const conceptId = searchParams.get("conceptId");
  const [drawerOpen, setDrawerOpen] = useState(false);

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

  return (
    <div className={`workspace-layout ${drawerOpen ? "workspace-layout-drawer-open" : ""}`}>
      <aside className="workspace-sidebar" id="workspace-sidebar">
        <ConceptSidebar activeId={conceptId} onNavigate={() => setDrawerOpen(false)} />
      </aside>
      <button
        type="button"
        className="workspace-drawer-backdrop"
        aria-label="Close concepts"
        tabIndex={drawerOpen ? 0 : -1}
        onClick={() => setDrawerOpen(false)}
      />
      <div className="workspace-main">
        <button
          type="button"
          className="workspace-drawer-toggle page-width"
          aria-controls="workspace-sidebar"
          aria-expanded={drawerOpen}
          onClick={() => setDrawerOpen(true)}
        >
          <SvgIcon name="folder" size={16} /> Concepts
        </button>
        <WorkspaceContent key={conceptId} conceptId={conceptId} />
      </div>
    </div>
  );
}

export default WorkspacePage;
