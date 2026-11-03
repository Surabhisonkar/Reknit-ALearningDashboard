import { AbsoluteFill, Img, Sequence, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";

import { layoutElements } from "./layouts.js";
import { FPS, HEIGHT, TRANSITION_FRAMES, WIDTH, storyboardTiming } from "./storyboardTiming.js";

/**
 * The Remotion composition for an animation storyboard. The app's Player
 * and the MP4 render service (animation-renderer/) both draw this same
 * file, so the video and the live preview always match. It may import only
 * `remotion`, `react` and files in this package.
 *
 * Props: { scenes, assetUrls } - scenes in the shape mapAnimation produces
 * (./mapAnimation.js), assetUrls mapping artifact id -> URL.
 */

const COLORS = {
  coral: { fill: "#ff795b", text: "#ffffff", soft: "#ffe3db" },
  teal: { fill: "#39b4a0", text: "#ffffff", soft: "#d5f3ec" },
  yellow: { fill: "#f8c554", text: "#3a2e2a", soft: "#fff3cd" },
  ink: { fill: "#3a2e2a", text: "#ffffff", soft: "#f1e8e4" },
};
const INK = "#3a2e2a";
const MUTED = "#735f59";
const PAPER = "#fff8f6";
const FONT = 'ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';

/** How a scene enters, driven by the previous scene's transitionToNext. */
function enterStyle(transition, frame) {
  const t = interpolate(frame, [0, TRANSITION_FRAMES], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  switch (transition) {
    case "slide":
      return { transform: `translateX(${(1 - t) * 100}%)` };
    case "wipe":
      return { clipPath: `inset(0 ${(1 - t) * 100}% 0 0)` };
    case "zoom":
      return { opacity: t, transform: `scale(${0.85 + 0.15 * t})` };
    case "fade":
      return { opacity: t };
    default:
      return {};
  }
}

function elementMotion(element, frame, fps) {
  const local = frame - Math.round(element.enterAt * fps);
  if (local < 0) return { opacity: 0, progress: 0 };
  const appear = interpolate(local, [0, 12], [0, 1], { extrapolateRight: "clamp" });
  switch (element.animation) {
    case "pop": {
      const s = spring({ frame: local, fps, config: { damping: 12, mass: 0.6 } });
      return { opacity: appear, transform: `scale(${s})`, progress: 1 };
    }
    case "slide":
      return { opacity: appear, transform: `translateY(${(1 - appear) * 40}px)`, progress: 1 };
    case "draw":
      return { opacity: 1, progress: interpolate(local, [0, 20], [0, 1], { extrapolateRight: "clamp" }) };
    case "pulse": {
      const pulse = 1 + 0.05 * Math.sin((local / fps) * Math.PI * 2);
      return { opacity: appear, transform: `scale(${pulse})`, progress: 1 };
    }
    case "highlight": {
      // Pops in, then a soft ring keeps pulsing out from it.
      const s = spring({ frame: local, fps, config: { damping: 14, mass: 0.6 } });
      return { opacity: appear, transform: `scale(${s})`, progress: 1, ring: (local % (fps * 1.4)) / (fps * 1.4) };
    }
    default:
      return { opacity: appear, progress: 1 };
  }
}

function ShapeElement({ element, motion }) {
  const color = COLORS[element.color] ?? COLORS.ink;
  const base = {
    position: "absolute",
    left: `${element.x}%`,
    top: `${element.y}%`,
    translate: "-50% -50%",
    opacity: motion.opacity,
    transform: motion.transform,
    fontFamily: FONT,
    textAlign: "center",
  };

  if (element.kind === "emoji") {
    return <div style={{ ...base, fontSize: 96, lineHeight: 1 }}>{element.text}</div>;
  }
  if (element.kind === "text") {
    return <div style={{ ...base, color: INK, fontSize: 34, fontWeight: 700, maxWidth: 420 }}>{element.text}</div>;
  }
  const circle = element.kind === "circle";
  const compact = Boolean(element.compact) && !circle;
  const shadow = "0 14px 30px rgba(102, 54, 40, 0.16)";
  const ringAlpha = motion.ring === undefined ? null : Math.round((1 - motion.ring) * 90).toString(16).padStart(2, "0");
  return (
    <div
      style={{
        ...base,
        alignItems: "center",
        background: color.fill,
        borderRadius: circle ? "50%" : 22,
        boxShadow: ringAlpha ? `${shadow}, 0 0 0 ${motion.ring * 26}px ${color.fill}${ringAlpha}` : shadow,
        color: color.text,
        display: "flex",
        flexDirection: "column",
        fontSize: compact ? 23 : 28,
        fontWeight: 800,
        gap: compact ? 3 : 6,
        height: circle ? 170 : "auto",
        justifyContent: "center",
        lineHeight: 1.15,
        // A layout may narrow a box so a crowded row fits; the label then wraps.
        maxWidth: circle ? 170 : Math.min(300, element.maxWidth ?? 300),
        minWidth: circle ? 170 : Math.min(160, element.maxWidth ?? 160),
        padding: circle ? 18 : compact ? "12px 16px" : element.maxWidth && element.maxWidth < 240 ? "20px 18px" : "24px 30px",
      }}
    >
      {element.icon && <span style={{ fontSize: compact ? 34 : 46, lineHeight: 1 }}>{element.icon}</span>}
      {element.text}
    </div>
  );
}

/** How far from an element's centre an arrow along (ux, uy) has to stop to clear its edge. */
function edgeDistance(element, ux, uy) {
  const ax = Math.abs(ux);
  const ay = Math.abs(uy);
  switch (element.kind) {
    case "circle":
      return 100;
    case "emoji":
      return 64;
    case "text":
      return ax * Math.min(220, element.text.length * 10 + 10) + ay * 30 + 16;
    default: {
      // Matches ShapeElement's box: 160-300px wide (or narrower in a crowded layout), ~80px tall (~140px with an icon).
      const widest = Math.min(300, element.maxWidth ?? 300);
      const halfWidth = Math.min(widest, Math.max(Math.min(160, widest), element.text.length * 16 + 60)) / 2;
      const halfHeight = (element.icon ? 70 : 46) * (element.compact ? 0.8 : 1);
      return Math.min(halfWidth / Math.max(ax, 0.01), halfHeight / Math.max(ay, 0.01)) + 16;
    }
  }
}

/** Arrows run between the centres of two elements, trimmed so they don't sit under the shapes. */
function ArrowElement({ element, byId, motion }) {
  const from = byId.get(element.fromId);
  const to = byId.get(element.toId);
  if (!from || !to) return null;
  const x1 = (from.x / 100) * WIDTH;
  const y1 = (from.y / 100) * HEIGHT;
  const x2 = (to.x / 100) * WIDTH;
  const y2 = (to.y / 100) * HEIGHT;
  const length = Math.hypot(x2 - x1, y2 - y1) || 1;
  const ux = (x2 - x1) / length;
  const uy = (y2 - y1) / length;
  const startTrim = Math.min(edgeDistance(from, ux, uy), length * 0.4);
  const endTrim = Math.min(edgeDistance(to, ux, uy), length * 0.4);
  const sx = x1 + ux * startTrim;
  const sy = y1 + uy * startTrim;
  const ex = x2 - ux * endTrim;
  const ey = y2 - uy * endTrim;
  const drawn = Math.hypot(ex - sx, ey - sy);
  const color = (COLORS[element.color] ?? COLORS.ink).fill;
  return (
    <svg width={WIDTH} height={HEIGHT} style={{ position: "absolute", inset: 0, opacity: motion.opacity }}>
      <defs>
        <marker id={`head-${element.id}`} viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
          <path d="M0 0 L10 5 L0 10 z" fill={color} />
        </marker>
      </defs>
      <line
        x1={sx}
        y1={sy}
        x2={ex}
        y2={ey}
        stroke={color}
        strokeWidth={7}
        strokeLinecap="round"
        strokeDasharray={drawn}
        strokeDashoffset={drawn * (1 - motion.progress)}
        markerEnd={motion.progress > 0.95 ? `url(#head-${element.id})` : undefined}
      />
      {element.text && motion.progress > 0.6 && (
        <g transform={`translate(${(sx + ex) / 2} ${(sy + ey) / 2})`} opacity={(motion.progress - 0.6) / 0.4}>
          <rect x={-(element.text.length * 7 + 18)} y={-20} width={element.text.length * 14 + 36} height={40} rx={20} fill="#ffffff" stroke={color} strokeWidth={3} />
          <text textAnchor="middle" dominantBaseline="central" fill={INK} fontFamily={FONT} fontSize={22} fontWeight={700}>
            {element.text}
          </text>
        </g>
      )}
    </svg>
  );
}

/** The caption comes in a word at a time, finishing within the first half of the scene. */
function Caption({ text, frame, durationFrames, fullStage }) {
  const words = text.split(/\s+/).filter(Boolean);
  const step = Math.min(4, Math.max(1, (durationFrames * 0.5 - 12) / Math.max(words.length, 1)));
  const boxIn = interpolate(frame, [8, 18], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div
      style={{
        position: "absolute",
        left: 64,
        right: 64,
        bottom: 40,
        background: "rgba(255, 255, 255, 0.92)",
        border: "2px solid #ead2ca",
        borderRadius: 20,
        color: MUTED,
        fontFamily: FONT,
        fontSize: 28,
        lineHeight: 1.4,
        opacity: boxIn,
        padding: "18px 26px",
        // Scenes without visual elements (older animations) give the caption the whole stage.
        ...(fullStage ? { top: 150, display: "flex", flexWrap: "wrap", alignContent: "center", fontSize: 36, color: INK } : {}),
      }}
    >
      {words.map((word, i) => (
        <span key={i} style={{ opacity: interpolate(frame, [12 + i * step, 18 + i * step], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }) }}>
          {word}{" "}
        </span>
      ))}
    </div>
  );
}

function Scene({ scene, index, total, previousTransition, assetUrls }) {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const elements = layoutElements(scene);
  const byId = new Map(elements.map((el) => [el.id, el]));
  const image = scene.assetArtifactIds?.length ? assetUrls?.[scene.assetArtifactIds[0]] : null;
  const titleIn = interpolate(frame, [4, 18], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const durationFrames = Math.round(scene.durationSeconds * fps);
  // A slow push-in keeps the frame alive between element entrances.
  const camera = interpolate(frame, [0, durationFrames], [1, 1.045], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill style={{ background: PAPER, overflow: "hidden", ...(index > 0 ? enterStyle(previousTransition, frame) : {}) }}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 12% 18%, ${COLORS.yellow.soft} 0, transparent 34%), radial-gradient(circle at 90% 85%, ${COLORS.teal.soft} 0, transparent 38%)`,
        }}
      />
      {image && <Img src={image} style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover", opacity: 0.28 }} />}

      <div style={{ position: "absolute", left: 64, right: 64, top: 44, display: "flex", alignItems: "center", gap: 18, opacity: titleIn, fontFamily: FONT }}>
        <span style={{ background: COLORS.coral.fill, borderRadius: 999, color: "#fff", fontSize: 22, fontWeight: 800, padding: "6px 16px" }}>
          {index + 1}/{total}
        </span>
        <h1 style={{ color: INK, fontSize: 46, fontWeight: 800, margin: 0 }}>{scene.title}</h1>
      </div>

      <AbsoluteFill style={{ transform: `scale(${camera})` }}>
        {elements.map((element) => {
          const motion = elementMotion(element, frame, fps);
          return element.kind === "arrow" ? (
            <ArrowElement key={element.id} element={element} byId={byId} motion={motion} />
          ) : (
            <ShapeElement key={element.id} element={element} motion={motion} />
          );
        })}
      </AbsoluteFill>

      {scene.narration && <Caption text={scene.narration} frame={frame} durationFrames={durationFrames} fullStage={elements.length === 0} />}
    </AbsoluteFill>
  );
}

export function StoryboardVideo({ scenes, assetUrls = {} }) {
  const { starts } = storyboardTiming(scenes);
  return (
    <AbsoluteFill style={{ background: PAPER }}>
      {scenes.map((scene, i) => (
        <Sequence key={scene.id} from={starts[i]} durationInFrames={Math.round(scene.durationSeconds * FPS)} name={scene.title}>
          <Scene
            scene={scene}
            index={i}
            total={scenes.length}
            previousTransition={i > 0 ? scenes[i - 1].transition : "none"}
            assetUrls={assetUrls}
          />
        </Sequence>
      ))}
    </AbsoluteFill>
  );
}
