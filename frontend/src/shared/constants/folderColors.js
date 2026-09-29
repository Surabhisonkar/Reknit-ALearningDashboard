/**
 * Folder colour registry - the one place that knows the palette keys the
 * backend accepts (FolderColor) and how each one is drawn. Components ask
 * this module for a class name; they never hard-code a colour, so a future
 * dark theme only has to redefine the CSS tokens behind these classes.
 */
export const FOLDER_COLORS = [
  { key: "coral", label: "Coral" },
  { key: "yellow", label: "Yellow" },
  { key: "teal", label: "Teal" },
  { key: "sky", label: "Sky" },
  { key: "violet", label: "Violet" },
  { key: "rose", label: "Rose" },
  { key: "green", label: "Green" },
  { key: "slate", label: "Slate" },
];

const KNOWN_KEYS = new Set(FOLDER_COLORS.map((c) => c.key));

/** CSS modifier class for a palette key; unknown or missing keys fall back to the neutral "unfiled" look. */
export function folderColorClass(key) {
  return KNOWN_KEYS.has(key) ? `folder-color-${key}` : "folder-color-none";
}

export function folderColorLabel(key) {
  return FOLDER_COLORS.find((c) => c.key === key)?.label ?? "No colour";
}

/** Picker value meaning "let the server pick the least-used palette colour". */
export const AUTO_COLOR = "auto";

/** Maps a picker value to what the API expects (null = server picks). */
export function toApiColor(value) {
  return value === AUTO_COLOR ? null : value;
}
