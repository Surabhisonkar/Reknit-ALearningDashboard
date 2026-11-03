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

const CUSTOM_COLOR = /^#[0-9a-f]{6}$/i;

/** True for a colour the user picked themselves (#rrggbb) rather than a palette key. */
export function isCustomColor(value) {
  return typeof value === "string" && CUSTOM_COLOR.test(value);
}

/**
 * CSS modifier class for a folder colour. Palette keys get their own class;
 * a custom #rrggbb gets "folder-color-custom" and its tokens come from
 * folderColorStyle; anything else falls back to the neutral "unfiled" look.
 */
export function folderColorClass(key) {
  if (KNOWN_KEYS.has(key)) return `folder-color-${key}`;
  return isCustomColor(key) ? "folder-color-custom" : "folder-color-none";
}

/**
 * Inline tokens for a custom colour, matching the palette classes: the colour
 * itself, a soft tint of it, and a dark shade of it readable on the tint.
 * Undefined for palette keys, whose tokens live in index.css.
 */
export function folderColorStyle(key) {
  if (!isCustomColor(key)) return undefined;
  return {
    "--fc": key,
    "--fc-soft": `color-mix(in srgb, ${key} 20%, white)`,
    "--fc-ink": `color-mix(in srgb, ${key} 42%, #1c1412)`,
  };
}

export function folderColorLabel(key) {
  if (isCustomColor(key)) return `Custom colour ${key}`;
  return FOLDER_COLORS.find((c) => c.key === key)?.label ?? "No colour";
}

/** Picker value meaning "let the server pick the least-used palette colour". */
export const AUTO_COLOR = "auto";

/** Maps a picker value to what the API expects (null = server picks). */
export function toApiColor(value) {
  return value === AUTO_COLOR ? null : value;
}
