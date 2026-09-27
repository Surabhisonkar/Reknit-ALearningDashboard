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
