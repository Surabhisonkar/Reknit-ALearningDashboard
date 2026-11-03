/**
 * Frame maths for the storyboard composition, shared by the app's Player
 * and the render service's calculateMetadata so both agree on length.
 */

export const FPS = 30;
export const WIDTH = 1280;
export const HEIGHT = 720;
/** Frames each scene overlaps the previous one, so transitions cross over. */
export const TRANSITION_FRAMES = 15;

/** Scene start frames and the total duration in frames. */
export function storyboardTiming(scenes) {
  let cursor = 0;
  const starts = scenes.map((scene, i) => {
    const start = cursor;
    cursor += Math.round(scene.durationSeconds * FPS) - (i < scenes.length - 1 ? TRANSITION_FRAMES : 0);
    return start;
  });
  return { starts, durationInFrames: Math.max(1, cursor) };
}
