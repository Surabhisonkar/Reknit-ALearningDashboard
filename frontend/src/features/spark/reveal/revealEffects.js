/**
 * How each visual type enters when a card is revealed (user decision: the
 * reveal is always animated, whatever the type). Keyed by visualization type;
 * a new type falls back to the fade. Styles live in index.css ("reveal-*"),
 * and all of them respect prefers-reduced-motion.
 */
const EFFECTS = Object.freeze({
  animation: "reveal-play",
  mind_map: "reveal-grow",
  diagram: "reveal-draw",
  image: "reveal-fade",
});

export function revealEffectClass(visualizationType) {
  return EFFECTS[visualizationType] ?? "reveal-fade";
}
