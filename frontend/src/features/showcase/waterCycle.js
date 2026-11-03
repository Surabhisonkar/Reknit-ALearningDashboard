import { mapVisualizationPayload } from "../../domain/visualizationMappers.js";
import waterCycleImage from "./water-cycle.svg";
import { ANIMATION, DIAGRAM, IMAGE, MIND_MAP, SHOWCASE_IMAGE_ID } from "./waterCycleData.js";

export { NOTE } from "./waterCycleData.js";

/**
 * The showcase tabs: each payload goes through the same mapper and renderer
 * as a saved concept, so what visitors see is what the app draws today.
 */
export const SHOWCASE_ASSET_URLS = { [SHOWCASE_IMAGE_ID]: waterCycleImage };

export const SHOWCASE_TABS = [
  { type: "diagram", label: "Diagram", visualization: mapVisualizationPayload(DIAGRAM) },
  { type: "image", label: "Illustration", visualization: mapVisualizationPayload(IMAGE) },
  { type: "mind_map", label: "Mind map", visualization: mapVisualizationPayload(MIND_MAP) },
  { type: "animation", label: "Animation", visualization: mapVisualizationPayload(ANIMATION) },
];
