/**
 * Scene layouts. A model is poor at choosing x/y positions, so a scene can
 * name a layout instead and every element that isn't an arrow is placed by
 * it, in order. Arrows still connect elements by id. A scene without a
 * layout ("free") keeps the positions it was given.
 *
 * Positions are percentages of the 16:9 frame. The band between the title
 * (top) and the caption (bottom) runs from roughly y=24 to y=72. A placer
 * can also cap an element's width (in px of the 1280px frame) so a crowded
 * row wraps its labels instead of overlapping, and mark it compact (smaller
 * text and icon) where boxes stack closely.
 */

export const LAYOUTS = ["free", "steps", "cycle", "compare", "before_after", "focus"];

const CENTRE_Y = 48;

function onEllipse(count, rx, ry, cy = CENTRE_Y) {
  return Array.from({ length: count }, (_, i) => {
    // Start at the top and go clockwise.
    const angle = -Math.PI / 2 + (i * 2 * Math.PI) / count;
    return { x: 50 + rx * Math.cos(angle), y: cy + ry * Math.sin(angle) };
  });
}

const FRAME_WIDTH = 1280;
const GAP = 24;

function inRow(count, left = 10, right = 90, y = CENTRE_Y) {
  const maxWidth = Math.floor(((right - left) / 100) * (FRAME_WIDTH / count)) - GAP;
  return Array.from({ length: count }, (_, i) => ({ x: left + ((right - left) * (i + 0.5)) / count, y, maxWidth, compact: count >= 5 }));
}

function inColumn(count, x) {
  if (count === 1) return [{ x, y: CENTRE_Y }];
  // Only as tall as needed: two boxes sit close to the middle, three or more use the whole band.
  const span = Math.min(47, 26 * (count - 1));
  const top = CENTRE_Y - span / 2;
  return Array.from({ length: count }, (_, i) => ({ x, y: top + (span * i) / (count - 1), compact: count >= 3 }));
}

const PLACERS = {
  steps: (n) => inRow(n),
  // Five or six round a loop need a wider, taller ring and narrower boxes to keep clear of each other.
  cycle: (n) =>
    n >= 5
      ? onEllipse(n, 33, 22, 49).map((spot) => ({ ...spot, maxWidth: 210, compact: true }))
      : onEllipse(n, 27, 20).map((spot) => ({ ...spot, maxWidth: 260 })),
  compare: (n) => {
    const left = Math.ceil(n / 2);
    return [...inColumn(left, 27), ...inColumn(n - left, 73)];
  },
  // The first element is the "before", the last the "after"; anything between sits in the middle.
  before_after: (n) => (n <= 2 ? inRow(n, 4, 96) : [{ x: 20, y: CENTRE_Y }, ...inColumn(n - 2, 50), { x: 80, y: CENTRE_Y }]),
  // The main idea centred near the top, everything else in a row beneath it.
  focus: (n) => [{ x: 50, y: 37 }, ...inRow(n - 1, 12, 88, 64)],
};

/** Returns the scene's elements with x/y filled in by its layout. */
export function layoutElements(scene) {
  const place = PLACERS[scene.layout];
  if (!place) return scene.elements;
  const placed = scene.elements.filter((el) => el.kind !== "arrow");
  const spots = place(placed.length);
  const at = new Map(placed.map((el, i) => [el.id, spots[i]]));
  return scene.elements.map((el) => (at.has(el.id) ? { ...el, ...at.get(el.id) } : el));
}
