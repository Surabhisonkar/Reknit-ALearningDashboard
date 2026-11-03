import { useState } from "react";

import { getConceptVersion } from "../../../api/conceptsApi.js";
import { useAuth } from "../../../auth";
import { SvgIcon } from "../../../shared/ui";

/**
 * "Source notes": the text the shown version's visual was generated from -
 * your own notes, or the AI explanation if you chose to visualize that.
 * Collapsed by default and fetched only when opened.
 */
export default function SourceNotesPanel({ conceptId, version }) {
  const { accessToken } = useAuth();
  const [state, setState] = useState({ status: "idle", text: null, version: null });

  function handleToggle(event) {
    if (!event.currentTarget.open || (state.version === version && state.status !== "error")) return;
    setState({ status: "loading", text: null, version });
    getConceptVersion(accessToken, conceptId, version)
      .then((json) => setState({ status: "ready", text: json.sourceText ?? null, version }))
      .catch(() => setState({ status: "error", text: null, version }));
  }

  // Switching versions while open shows that version's text the next time it opens.
  const stale = state.version !== version;

  return (
    <details className="notes-panel source-notes page-width" onToggle={handleToggle} key={version}>
      <summary>
        <SvgIcon name="book" size={18} />
        <span>Source notes</span>
      </summary>
      <div className="source-notes-body">
        {(state.status === "loading" || stale) && <p className="source-notes-hint">Loading...</p>}
        {!stale && state.status === "error" && <p className="source-notes-hint">Couldn&apos;t load the source notes.</p>}
        {!stale && state.status === "ready" &&
          (state.text?.trim() ? (
            <>
              <p className="source-notes-hint">The text this visual was made from.</p>
              <p className="source-notes-text">{state.text}</p>
            </>
          ) : (
            <p className="source-notes-hint">No source notes were kept for this version.</p>
          ))}
      </div>
    </details>
  );
}
