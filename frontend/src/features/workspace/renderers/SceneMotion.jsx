const TRACK_START = 24;
const TRACK_END = 376;
const TRACK_Y = 30;
const RING_RADIUS = 34;
const RING_LENGTH = 2 * Math.PI * RING_RADIUS;

function stopX(index, count) {
  if (count <= 1) return (TRACK_START + TRACK_END) / 2;
  return TRACK_START + ((TRACK_END - TRACK_START) * index) / (count - 1);
}

/**
 * The coded stand-in for a scene picture: drawn entirely with SVG and
 * CSS, no network call, so it works whenever the image model does not.
 * Every moving part carries information rather than decoration:
 * - the ring around the step number fills over the scene's duration
 *   (it is the "how long is left" timer, and pauses with playback);
 * - the marker travels along the track from the previous step to this
 *   one, and steps already seen stay filled.
 * The track's y position (30) is mirrored in index.css's markerTravel keyframes.
 * Motion is switched off under prefers-reduced-motion (see index.css);
 * the final state of each part is still shown.
 */
function SceneMotion({ index, count, durationSeconds, playing }) {
  const fractionAt = (i) => (count <= 1 ? 1 : i / (count - 1));
  // Each scene mounts fresh, so "travel" is an entrance animation from the previous stop to this one.
  const previous = Math.max(index - 1, 0);
  const travel = {
    "--from-x": `${stopX(previous, count)}px`,
    "--to-x": `${stopX(index, count)}px`,
    "--from-scale": fractionAt(previous),
    "--to-scale": fractionAt(index),
  };

  return (
    <div className="scene-motion" aria-hidden="true">
      <svg viewBox="0 0 100 100" className="scene-motion-ring">
        <circle cx="50" cy="50" r={RING_RADIUS} className="scene-motion-ring-track" />
        <circle
          // Remount per scene so the fill restarts from empty.
          key={index}
          cx="50"
          cy="50"
          r={RING_RADIUS}
          className="scene-motion-ring-fill"
          style={{
            strokeDasharray: RING_LENGTH,
            "--ring-length": RING_LENGTH,
            animationDuration: `${durationSeconds}s`,
            animationPlayState: playing ? "running" : "paused",
          }}
        />
        <text x="50" y="50" className="scene-motion-number">
          {index + 1}
        </text>
      </svg>

      <div className="scene-motion-progress">
        <span className="scene-motion-caption">
          Step {index + 1} of {count}
        </span>
        <svg viewBox="0 0 400 60" className="scene-motion-track" preserveAspectRatio="xMinYMid meet">
          <line x1={TRACK_START} y1={TRACK_Y} x2={TRACK_END} y2={TRACK_Y} className="scene-motion-line" />
          <rect
            x={TRACK_START}
            y={TRACK_Y - 3}
            width={TRACK_END - TRACK_START}
            height="6"
            className="scene-motion-line-done"
            style={travel}
          />
          {Array.from({ length: count }, (_, i) => (
            <circle
              key={i}
              cx={stopX(i, count)}
              cy={TRACK_Y}
              r="9"
              className={i < index ? "scene-motion-stop scene-motion-stop-done" : "scene-motion-stop"}
            />
          ))}
          <g className="scene-motion-marker" style={travel}>
            <circle r="20" className="scene-motion-marker-pulse" />
            <circle r="13" className="scene-motion-marker-dot" />
          </g>
        </svg>
      </div>
    </div>
  );
}

export default SceneMotion;
