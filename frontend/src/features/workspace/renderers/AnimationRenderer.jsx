import { useState } from "react";
import { Player } from "@remotion/player";
import { FPS, HEIGHT, StoryboardVideo, WIDTH, storyboardTiming } from "@reknit/storyboard";

import { useArtifactUrls } from "./artifactUrls.js";

/**
 * Consumes the shape produced by domain/visualizationMappers.js's
 * mapAnimation. Plays the MP4 the render service stored in S3 when there
 * is one; otherwise (no renderer configured, demo mode, older animations)
 * plays the very same Remotion composition live in the browser.
 */
function AnimationRenderer({ visualization }) {
  const { scenes, videoArtifactId } = visualization;
  const sceneAssetIds = scenes.flatMap((s) => s.assetArtifactIds.slice(0, 1));
  const urls = useArtifactUrls(videoArtifactId ? [videoArtifactId, ...sceneAssetIds] : sceneAssetIds);
  const [preferLive, setPreferLive] = useState(false);
  const videoUrl = videoArtifactId ? urls[videoArtifactId] : null;

  if (scenes.length === 0) return null;
  const { durationInFrames } = storyboardTiming(scenes);
  const showVideo = videoUrl && !preferLive;

  return (
    <div className="animation-renderer">
      {showVideo ? (
        <video className="animation-video" src={videoUrl} controls playsInline preload="metadata" />
      ) : (
        <Player
          className="animation-player"
          component={StoryboardVideo}
          inputProps={{ scenes, assetUrls: urls }}
          durationInFrames={durationInFrames}
          fps={FPS}
          compositionWidth={WIDTH}
          compositionHeight={HEIGHT}
          style={{ width: "100%", aspectRatio: `${WIDTH} / ${HEIGHT}` }}
          controls
          autoPlay
          clickToPlay
          acknowledgeRemotionLicense
        />
      )}

      <div className="animation-meta">
        <span>
          {scenes.length} scenes · {Math.round(durationInFrames / FPS)}s
        </span>
        {videoUrl && (
          <button type="button" className="viz-chip" onClick={() => setPreferLive((v) => !v)}>
            {preferLive ? "Show MP4" : "Show live preview"}
          </button>
        )}
      </div>

      <ol className="animation-storyboard">
        {scenes.map((scene, i) => (
          <li key={scene.id}>
            <span className="animation-storyboard-index">{i + 1}</span>
            <div>
              <strong>{scene.title}</strong>
              <p>{scene.narration}</p>
            </div>
            <span className="animation-storyboard-time">{scene.durationSeconds}s</span>
          </li>
        ))}
      </ol>
    </div>
  );
}

export default AnimationRenderer;
