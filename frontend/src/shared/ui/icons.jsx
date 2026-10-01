/**
 * Inline SVG icon registry (the project has no icon library). Icons are
 * always decorative: an icon-only button carries its name itself, via
 * aria-label and title, as the Spark controls do. Adding an icon = one entry.
 */
const PATHS = {
  shuffle: "M16 3h5v5M4 20 21 3M21 16v5h-5M15 15l6 6M4 4l5 5",
  folder: "M3 6.5A1.5 1.5 0 0 1 4.5 5H9l2 2.5h8.5A1.5 1.5 0 0 1 21 9v9.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z",
  clock: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2",
  sparkle: "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z",
  book: "M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5zM4 20.5A2.5 2.5 0 0 0 6.5 23H20v-5",
  chat: "M4 5h16v11H9l-5 4zM8 9.5h8M8 12.5h5",
  speaker: "M4 9h4l5-4v14l-5-4H4zM16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12",
  speakerOff: "M4 9h4l5-4v14l-5-4H4zM17 9l5 6M22 9l-5 6",
  send: "M4 12l16-8-6 16-3-6.5zM11 13.5 20 4",
  close: "M6 6l12 12M18 6 6 18",
  chevronUp: "M6 15l6-6 6 6",
  chevronDown: "M6 9l6 6 6-6",
  chevronLeft: "M15 6l-6 6 6 6",
  chevronRight: "M9 6l6 6-6 6",
  layers: "M12 3 3 8l9 5 9-5zM3 13l9 5 9-5M3 18l9 5 9-5",
  note: "M6 3h9l4 4v14H6zM14 3v5h5M9 12h7M9 16h5",
  image: "M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4M15.5 9.5h.01",
  trash: "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13",
};

export function SvgIcon({ name, size = 22, className = "" }) {
  const d = PATHS[name];
  if (!d) return null;
  return (
    <svg
      className={`svg-icon ${className}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={d} />
    </svg>
  );
}
