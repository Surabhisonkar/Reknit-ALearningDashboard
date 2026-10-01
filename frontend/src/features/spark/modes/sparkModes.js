import { createFolderSource, createShuffleSource } from "./sparkSources.js";

/**
 * Spark's modes. Looked up by id, never switched on. A mode that isn't
 * `available` yet stays visible but only says "coming soon".
 */
export const SPARK_MODES = Object.freeze([
  {
    id: "shuffle",
    label: "Shuffle",
    icon: "shuffle",
    available: true,
    needsFolder: false,
    createSource: (accessToken) => createShuffleSource(accessToken),
  },
  {
    id: "folder",
    label: "Folder",
    icon: "folder",
    available: true,
    needsFolder: true,
    createSource: (accessToken, { folder }) => createFolderSource(accessToken, folder),
  },
  // Needs review history, which Phase 9 builds.
  { id: "due", label: "Due for review", icon: "clock", available: false, needsFolder: false },
]);

export function findSparkMode(id) {
  return SPARK_MODES.find((mode) => mode.id === id) ?? SPARK_MODES[0];
}
