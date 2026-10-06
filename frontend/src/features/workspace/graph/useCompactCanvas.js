import { useSyncExternalStore } from "react";

/** Same breakpoint as index.css's phone rules. */
const QUERY = "(max-width: 760px)";

function subscribe(onChange) {
  const media = window.matchMedia(QUERY);
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

/**
 * True on phone-width screens, where a graph canvas is tall and narrow.
 * Layouts use it to arrange nodes in a portrait shape that stays readable
 * without zooming, instead of shrinking a wide layout to fit.
 */
export function useCompactCanvas() {
  return useSyncExternalStore(
    subscribe,
    () => window.matchMedia(QUERY).matches,
    () => false
  );
}
