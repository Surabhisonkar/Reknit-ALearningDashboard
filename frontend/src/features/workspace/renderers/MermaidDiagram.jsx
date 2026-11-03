import { useEffect, useId, useState } from "react";

let mermaidPromise = null;

/** Mermaid is large, so it loads the first time a diagram is shown, and only once. */
function loadMermaid() {
  mermaidPromise ??= import("mermaid").then(({ default: mermaid }) => {
    mermaid.initialize({
      startOnLoad: false,
      // "strict" sanitises labels and disables click handlers; the backend also rejects them.
      securityLevel: "strict",
      theme: "base",
      // Named, not "inherit": Mermaid sizes each label box by measuring the text off-screen,
      // so it has to measure in the same font the diagram is drawn in.
      fontFamily: "'Plus Jakarta Sans', sans-serif",
      themeVariables: {
        primaryColor: "#d5f3ec",
        primaryBorderColor: "#39b4a0",
        primaryTextColor: "#3a2e2a",
        lineColor: "#735f59",
        secondaryColor: "#fff3cd",
        tertiaryColor: "#fff0eb",
        fontSize: "15px",
      },
    });
    return mermaid;
  });
  return mermaidPromise;
}

/**
 * Renders a version-2 diagram: Mermaid source drawn as SVG, so every label
 * and arrow is real text. "Edit" opens the source beside the picture and
 * redraws as you type; a syntax error keeps the last good drawing on
 * screen. Edits live in this view only - they aren't saved to the concept.
 */
function MermaidDiagram({ visualization }) {
  const [source, setSource] = useState(visualization.source);
  const [svg, setSvg] = useState("");
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const renderId = `mmd-${useId().replace(/[^a-zA-Z0-9]/g, "")}`;

  useEffect(() => {
    let cancelled = false;
    const timer = setTimeout(async () => {
      try {
        const mermaid = await loadMermaid();
        // Measuring before the web font arrives gives boxes too narrow for the real text.
        await document.fonts?.ready;
        const { svg: drawn } = await mermaid.render(renderId, source);
        if (!cancelled) {
          setSvg(drawn);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err?.message?.split("\n")[0] ?? "This diagram couldn't be drawn.");
      }
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [source, renderId]);

  const changed = source !== visualization.source;

  return (
    <div className={`diagram-renderer mermaid-diagram ${editing ? "mermaid-diagram-editing" : ""}`}>
      <div className="mermaid-toolbar">
        {visualization.caption && <p className="mermaid-caption">{visualization.caption}</p>}
        <div className="mermaid-toolbar-actions">
          {changed && (
            <button type="button" className="viz-chip" onClick={() => setSource(visualization.source)}>
              Reset
            </button>
          )}
          <button type="button" className="viz-chip" aria-pressed={editing} onClick={() => setEditing((e) => !e)}>
            {editing ? "Done" : "Edit"}
          </button>
        </div>
      </div>

      <div className="mermaid-body">
        {svg ? (
          <div
            className="mermaid-canvas"
            role="img"
            aria-label={visualization.caption || "Diagram"}
            // Mermaid's own output, rendered under securityLevel "strict" (DOMPurify-sanitised).
            dangerouslySetInnerHTML={{ __html: svg }}
          />
        ) : (
          <div className="mermaid-canvas mermaid-placeholder" aria-hidden="true" />
        )}

        {editing && (
          <label className="mermaid-editor">
            <span>Mermaid source</span>
            <textarea value={source} onChange={(event) => setSource(event.target.value)} spellCheck={false} />
            {error && <small className="mermaid-error" role="status">{error}</small>}
          </label>
        )}
      </div>

      {!editing && error && !svg && <p className="visualization-error">{error}</p>}
    </div>
  );
}

export default MermaidDiagram;
