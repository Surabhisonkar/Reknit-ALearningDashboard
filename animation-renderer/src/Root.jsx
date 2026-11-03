import { Composition } from "remotion";

// The same composition and mapper the frontend uses, so the MP4 matches the in-app preview.
import { FPS, HEIGHT, StoryboardVideo, WIDTH, mapAnimation, storyboardTiming } from "@reknit/storyboard";

const EMPTY = { type: "animation", version: 2, scenes: [], videoArtifactId: "" };

/** Input props are the backend's raw payload; map it exactly as the frontend does before drawing. */
function StoryboardFromPayload({ storyboard, assetUrls }) {
  return <StoryboardVideo scenes={mapAnimation(storyboard).scenes} assetUrls={assetUrls} />;
}

export function RemotionRoot() {
  return (
    <Composition
      id="Storyboard"
      component={StoryboardFromPayload}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
      durationInFrames={FPS}
      defaultProps={{ storyboard: EMPTY, assetUrls: {} }}
      calculateMetadata={({ props }) => ({
        durationInFrames: storyboardTiming(mapAnimation(props.storyboard).scenes).durationInFrames,
      })}
    />
  );
}
