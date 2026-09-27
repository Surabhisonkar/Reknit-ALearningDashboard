import { useEffect, useRef, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";
import { ProgressDots } from "../../../shared/ui/ProgressDots.jsx";

const TRANSITION_CLASS = { fade: "scene-fade", slide: "scene-slide", cut: "scene-cut", none: "" };

function SceneImage({ artifactId }) {
  const { accessToken } = useAuth();
  const [url, setUrl] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getArtifactDownloadUrl(accessToken, artifactId)
      .then((res) => !cancelled && setUrl(res.downloadUrl))
      .catch(() => !cancelled && setUrl(null));
    return () => {
      cancelled = true;
    };
  }, [accessToken, artifactId]);

  if (!url) return <div className="scene-image-placeholder" aria-hidden="true" />;
  return <img src={url} alt="" className="scene-image" />;
}

/** Consumes the shape produced by domain/visualizationMappers.js's mapAnimation. */
function AnimationRenderer({ visualization }) {
  const { scenes } = visualization;
  const [index, setIndex] = useState(0);
  const [playing, setPlaying] = useState(true);
  const timerRef = useRef(null);

  const scene = scenes[index];

  useEffect(() => {
    if (!playing || !scene) return undefined;
    timerRef.current = setTimeout(() => {
      setIndex((i) => (i + 1 < scenes.length ? i + 1 : i));
      if (index + 1 >= scenes.length) setPlaying(false);
    }, scene.durationSeconds * 1000);
    return () => clearTimeout(timerRef.current);
  }, [playing, index, scene, scenes.length]);

  if (!scene) return null;

  return (
    <div className="animation-renderer">
      <div className={`animation-scene ${TRANSITION_CLASS[scene.transition] ?? ""}`} key={scene.id}>
        {scene.assetArtifactIds.length > 0 && <SceneImage artifactId={scene.assetArtifactIds[0]} />}
        <h3>{scene.title}</h3>
        <p>{scene.narration}</p>
      </div>

      <div className="animation-controls">
        <button type="button" onClick={() => setIndex((i) => Math.max(0, i - 1))} disabled={index === 0}>
          Previous
        </button>
        <button type="button" onClick={() => setPlaying((p) => !p)}>
          {playing ? "Pause" : "Play"}
        </button>
        <button
          type="button"
          onClick={() => setIndex((i) => Math.min(scenes.length - 1, i + 1))}
          disabled={index === scenes.length - 1}
        >
          Next
        </button>
      </div>

      <ProgressDots
        count={scenes.length}
        activeIndex={index}
        onSelect={setIndex}
        ariaLabel="Scenes"
        getKey={(i) => scenes[i].id}
      />
    </div>
  );
}

export default AnimationRenderer;
