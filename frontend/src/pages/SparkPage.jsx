import { useState } from "react";

import { useAuth } from "../auth";
import { SparkModeBar, SparkSession, findSparkMode, useSparkFolders } from "../features/spark";
import { NarratorProvider } from "../shared/narration";

/**
 * Spark: revisit what you've saved, reels-style. Each mode/folder choice
 * mounts a fresh session (via `key`), so switching never leaks old state.
 */
function SparkPage() {
  const { accessToken } = useAuth();
  const folders = useSparkFolders(accessToken);
  const [modeId, setModeId] = useState("shuffle");
  const [folder, setFolder] = useState("");
  const mode = findSparkMode(modeId);
  const needsChoice = mode.needsFolder && !folder;

  return (
    <NarratorProvider>
      <main className="spark-page">
        <SparkModeBar modeId={mode.id} onModeChange={setModeId} folders={folders} folder={folder} onFolderChange={setFolder} />
        {needsChoice ? (
          <div className="spark-state">Choose a folder to deep-dive into.</div>
        ) : (
          <SparkSessionForMode key={`${mode.id}:${folder}`} mode={mode} folder={folder} accessToken={accessToken} />
        )}
      </main>
    </NarratorProvider>
  );
}

/** Creates the mode's source once per mount, then runs the session. */
function SparkSessionForMode({ mode, folder, accessToken }) {
  const [source] = useState(() => mode.createSource(accessToken, { folder }));
  return <SparkSession accessToken={accessToken} source={source} />;
}

export default SparkPage;
