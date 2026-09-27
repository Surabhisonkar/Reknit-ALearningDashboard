import { useNavigate } from "react-router-dom";
import { useCallback, useRef } from "react";

import { useSparkFeed, SparkAnimationCard, SparkGameCard } from "../features/spark";
import { Pill } from "../shared/ui";

/**
 * Spark: an Instagram-Reels-style vertical feed of your saved animations,
 * with a quick mini-game interleaved every few cards to keep your
 * attention. Choose a specific library to revisit, or let it pull a
 * random mix from everything you've saved.
 */
function SparkPage() {
  const navigate = useNavigate();
  const { folders, source, setSource, items, status, loadMore } = useSparkFeed();
  const reelRef = useRef(null);
  const openConcept = useCallback((id) => navigate(`/workspace?conceptId=${id}`), [navigate]);

  function handleScroll(event) {
    const el = event.currentTarget;
    const nearEnd = el.scrollTop + el.clientHeight >= el.scrollHeight - el.clientHeight * 0.6;
    if (nearEnd && (status === "ready" || status === "loading-more")) {
      loadMore();
    }
  }

  return (
    <main className="spark-page">
      <div className="spark-topbar page-width">
        <div>
          <Pill tone="coral">SPARK</Pill>
          <p className="spark-subtitle">Short visual replays of what you've saved, with a quick game in between.</p>
        </div>
        <div className="spark-source-toggle">
          <button
            type="button"
            className={`tab ${source === "all" ? "active" : ""}`}
            onClick={() => setSource("all")}
          >
            Random, all libraries
          </button>
          <select
            value={source === "all" ? "" : source}
            onChange={(event) => setSource(event.target.value || "all")}
            disabled={folders.length === 0}
          >
            <option value="">Choose a library...</option>
            {folders.map((folder) => (
              <option key={folder} value={folder}>
                {folder}
              </option>
            ))}
          </select>
        </div>
      </div>

      {status === "loading" && (
        <div className="spark-status page-width">
          <p>Loading your Spark feed...</p>
        </div>
      )}

      {status === "error" && (
        <div className="spark-status page-width">
          <h1>Couldn't load Spark</h1>
          <p>Please try refreshing the page.</p>
        </div>
      )}

      {status === "empty" && (
        <div className="spark-status page-width">
          <h1>Nothing to play yet</h1>
          <p>
            Spark plays back your saved <strong>animation</strong> visualizations. Create one from a topic to see it
            here.
          </p>
          <button type="button" className="button" onClick={() => navigate("/create")}>
            Create a concept
          </button>
        </div>
      )}

      {items.length > 0 && (
        <div className="spark-reel" ref={reelRef} onScroll={handleScroll}>
          {items.map((item) =>
            item.kind === "concept" ? (
              <SparkAnimationCard key={item.key} concept={item.concept} onOpen={openConcept} />
            ) : (
              <SparkGameCard key={item.key} />
            )
          )}
          {status === "loading-more" && (
            <div className="spark-slide spark-slide-loading">
              <p>Loading more...</p>
            </div>
          )}
        </div>
      )}
    </main>
  );
}

export default SparkPage;
