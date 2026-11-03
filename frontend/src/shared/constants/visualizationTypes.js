/** Display labels for backend visualization types - one source for every screen that shows a type badge. */
export const VISUALIZATION_TYPE_LABELS = {
  mind_map: "Mind Map",
  diagram: "Diagram",
  animation: "Animation",
  image: "Illustration",
};

export function visualizationTypeLabel(type) {
  return VISUALIZATION_TYPE_LABELS[type] ?? type;
}

/**
 * What the Visualize button's dropdown offers, in menu order. `type` is the
 * backend's preferredVisualizationType; the hint is there to help someone
 * pick the form that suits their concept.
 */
export const VISUALIZE_OPTIONS = [
  {
    type: "diagram",
    label: "Structured diagram",
    hint: "Processes, systems and flows, with exact labels and arrows.",
  },
  {
    type: "image",
    label: "Illustration",
    hint: "A picture of a concrete thing, with its parts labelled.",
  },
  {
    type: "mind_map",
    label: "Mind map",
    hint: "A main idea and its branches. Expand, edit and rearrange them.",
  },
  {
    type: "animation",
    label: "Animation",
    hint: "A short video, scene by scene, for steps that happen in order.",
  },
];
