import { useCallback, useEffect, useRef, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";
import { ProgressDots } from "../../../shared/ui/ProgressDots.jsx";
import SceneMotion from "./SceneMotion.jsx";

const TRANSITION_CLASS = { fade: "scene-fade", slide: "scene-slide", cut: "scene-cut", none: "" };

/** The same sentence used as both title and narration (rule-based scenes) is shown once. */
function sameText(a, b) {
  const normalize = (text) => text.trim().replace(/[.!?…]+$/, "").toLowerCase();
  return normalize(a) === normalize(b);
}

/**
 * A scene's generated picture, when it has one and it can be shown.
 * `onUnavailable` fires if the URL cannot be fetched or the image itself
 * fails to load, so the scene can fall back to coded motion.
 */
function SceneImage({ artifactId, onUnavailable }) {
  const { accessToken } = useAuth();
  const [url, setUrl] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getArtifactDownloadUrl(accessToken, artifactId)
      .then((res) => !cancelled && setUrl(res.downloadUrl))
      .catch(() => !cancelled && onUnavailable());
    return () => {
      cancelled = true;
    };
  }, [accessToken, artifactId, onUnavailable]);

  if (!url) return <div className="scene-image-placeholder" aria-hidden="true" />;
  return <img src={url} alt="" className="scene-image" onError={onUnavailable} />;
}

/**
 * One scene's visual: its generated picture if there is a usable one,
 * otherwise SceneMotion. Keyed per scene by the caller, so "this picture
 * failed" never leaks into the next scene.
 */
function SceneVisual({ scene, index, count, playing }) {
  const [pictureFailed, setPictureFailed] = useState(false);
  const markFailed = useCallback(() => setPictureFailed(true), []);
  const artifactId = scene.assetArtifactIds[0];

  if (artifactId && !pictureFailed) {
    return <SceneImage artifactId={artifactId} onUnavailable={markFailed} />;
  }
  return <SceneMotion index={index} count={count} durationSeconds={scene.durationSeconds} playing={playing} />;
}

/**
 * Consumes the shape produced by domain/visualizationMappers.js's
 * mapAnimation. Plays every animation the backend can produce: scenes
 * with AI pictures, scenes whose pictures could not be generated, and
 * the rule-based failsafe (which never has pictures).
 */
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

  const titleWords = scene.title.split(/\s+/).filter(Boolean);

  return (
    <div className="animation-renderer">
      <div className={`animation-scene ${TRANSITION_CLASS[scene.transition] ?? ""}`} key={scene.id}>
        <SceneVisual scene={scene} index={index} count={scenes.length} playing={playing} />
        <h3 aria-label={scene.title}>
          {titleWords.map((word, i) => (
            <span key={i} className="scene-word" style={{ animationDelay: `${i * 70}ms` }} aria-hidden="true">
              {word}
            </span>
          ))}
        </h3>
        {!sameText(scene.title, scene.narration) && <p className="scene-narration">{scene.narration}</p>}
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
