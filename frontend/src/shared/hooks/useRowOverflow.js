import { useEffect, useRef, useState } from "react";

function sameSet(a, b) {
  if (a.size !== b.size) return false;
  for (const value of a) if (!b.has(value)) return false;
  return true;
}

/**
 * Reports which children of a wrapping flex row fell past the first line.
 * Put `rowRef` on the row and `data-overflow-key` on each child that counts.
 * Both the row and its parent (`observeRef`) are watched: the parent always
 * has a box, even when the row uses `display: contents` on small screens,
 * and the row's own width changes when siblings beside it appear.
 *
 * @param {string} signature changes whenever the set of children changes, so
 *   the row is measured again even if its size didn't change.
 * @returns {{ rowRef, observeRef, hiddenKeys: Set<string> }}
 */
export function useRowOverflow(signature) {
  const rowRef = useRef(null);
  const observeRef = useRef(null);
  const [hiddenKeys, setHiddenKeys] = useState(() => new Set());

  useEffect(() => {
    const row = rowRef.current;
    const observed = observeRef.current ?? row;
    if (!row || !observed || typeof ResizeObserver === "undefined") return undefined;

    function measure() {
      const items = [...row.querySelectorAll(":scope > [data-overflow-key]")];
      const firstTop = items[0]?.offsetTop ?? 0;
      const next = new Set(items.filter((item) => item.offsetTop > firstTop + 1).map((item) => item.dataset.overflowKey));
      setHiddenKeys((prev) => (sameSet(prev, next) ? prev : next));
    }

    // ResizeObserver calls back once right after observe(), so this also measures on mount.
    // Watch the row too: its width changes when siblings (e.g. a "More" button) appear,
    // even though the parent's size doesn't.
    const observer = new ResizeObserver(measure);
    observer.observe(observed);
    if (observed !== row) observer.observe(row);
    return () => observer.disconnect();
  }, [signature]);

  return { rowRef, observeRef, hiddenKeys };
}
