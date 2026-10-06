/** Display labels for backend visualization types - one source for every screen that shows a type badge. */
export const VISUALIZATION_TYPE_LABELS = {
  mind_map: "Mind Map",
  diagram: "Diagram",
  animation: "Animation",
  image: "Image",
};

export function visualizationTypeLabel(type) {
  return VISUALIZATION_TYPE_LABELS[type] ?? type;
}

/**
 * What Capture lets the user ask for. "auto" (the AI picks the type that
 * fits the text) stays the default; the rest are optional overrides.
 * `value` is sent as-is in preferredVisualizationType. An "image" request
 * may come back as a diagram when the picture can't be generated - hence
 * the combined label.
 */
export const VISUALIZATION_TYPE_CHOICES = [
  { value: "auto", label: "Let AI choose" },
  { value: "mind_map", label: "Mind map" },
  { value: "image", label: "Image / diagram" },
  { value: "animation", label: "Animation" },
];

export const DEFAULT_VISUALIZATION_TYPE_CHOICE = "auto";
