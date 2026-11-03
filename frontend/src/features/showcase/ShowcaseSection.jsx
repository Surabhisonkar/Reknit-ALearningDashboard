import { Pill } from "../../shared/ui/Pill";
import { VisualizationRenderer } from "../workspace";
import { NOTE, SHOWCASE_ASSET_URLS, SHOWCASE_TABS } from "./waterCycle.js";

const WORD_COUNT = [NOTE.title, ...NOTE.points].join(" ").split(/\s+/).length;

/**
 * Raw notes on the left, the same notes drawn four ways on the right. Each
 * tab renders with the app's real renderers; the heavy ones (mind map,
 * animation) load only when their tab is opened.
 */
export function ShowcaseSection({ activeType, onSelect }) {
  const active = SHOWCASE_TABS.find((tab) => tab.type === activeType) ?? SHOWCASE_TABS[0];

  return (
    <section className="showcase page-width tactile-panel" id="showcase" aria-label="Example: one note, four visuals">
      <div className="raw-note">
        <div className="note-heading">
          <Pill tone="yellow">YOUR NOTES</Pill>
          <span className="note-pin" />
        </div>
        <h3>{NOTE.title}</h3>
        <ol>
          {NOTE.points.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </ol>
        <small>
          {WORD_COUNT} words
        </small>
      </div>

      {/* Notes flow into the visuals: right on wide screens, down when the cards stack. */}
      <div className="transform-arrow" aria-hidden="true">
        <svg viewBox="0 0 64 24" width="64" height="24">
          <path d="M2 12h54" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeDasharray="1 7" />
          <path d="M50 5l9 7-9 7" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>

      <div className="showcase-output">
        <div className="showcase-tabs" role="tablist" aria-label="Visualization type">
          {SHOWCASE_TABS.map((tab) => (
            <button
              key={tab.type}
              type="button"
              role="tab"
              id={`showcase-tab-${tab.type}`}
              aria-selected={tab.type === active.type}
              aria-controls="showcase-panel"
              className={`showcase-tab ${tab.type === active.type ? "showcase-tab-active" : ""}`}
              onClick={() => onSelect(tab.type)}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div className="showcase-panel" id="showcase-panel" role="tabpanel" aria-labelledby={`showcase-tab-${active.type}`}>
          <VisualizationRenderer key={active.type} visualization={active.visualization} assetUrls={SHOWCASE_ASSET_URLS} />
        </div>
      </div>
    </section>
  );
}
