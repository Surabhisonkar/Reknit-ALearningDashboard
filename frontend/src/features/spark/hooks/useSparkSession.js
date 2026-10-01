import { useCallback, useEffect, useRef, useState } from "react";

import { composeFeed } from "../feed/composeFeed.js";

const PAGE_SIZE = 8;
const PREFETCH_WHEN_LEFT = 3;

/**
 * One endless Spark session over one source. Mount a new session (via
 * `key`) to switch mode or folder - state never has to be reset by hand.
 * When every concept has been shown, the next page starts another round.
 */
export function useSparkSession(source, { picker, gameChance, random = Math.random }) {
  const [items, setItems] = useState([]);
  const [index, setIndex] = useState(0);
  const [status, setStatus] = useState("loading"); // loading | ready | empty | error
  // Only the card on stage can be revealed; moving away shows its teaser again (user decision).
  const [revealedKey, setRevealedKey] = useState(null);
  const cursor = useRef({ seen: new Set(), seq: 0, lastGameId: null, loading: false });

  const loadPage = useCallback(async () => {
    const c = cursor.current;
    if (c.loading) return;
    c.loading = true;
    try {
      let concepts = await source.nextPage([...c.seen], PAGE_SIZE);
      if (concepts.length === 0 && c.seen.size > 0) {
        c.seen = new Set(); // everything shown once: start another round
        concepts = await source.nextPage([], PAGE_SIZE);
      }
      concepts.forEach((concept) => c.seen.add(concept.id));
      const composed = composeFeed(concepts, {
        picker,
        gameChance,
        random,
        startSeq: c.seq,
        lastGameId: c.lastGameId,
      });
      c.seq = composed.nextSeq;
      c.lastGameId = composed.lastGameId;
      setItems((prev) => [...prev, ...composed.items]);
      setStatus((prev) => (prev === "loading" ? (composed.items.length > 0 ? "ready" : "empty") : prev));
    } catch {
      setStatus((prev) => (prev === "loading" ? "error" : prev));
    } finally {
      c.loading = false;
    }
  }, [source, picker, gameChance, random]);

  useEffect(() => {
    loadPage();
  }, [loadPage]);

  const next = useCallback(() => {
    setRevealedKey(null);
    setIndex((i) => Math.min(i + 1, Math.max(items.length - 1, 0)));
    if (index >= items.length - PREFETCH_WHEN_LEFT) loadPage();
  }, [index, items.length, loadPage]);

  const previous = useCallback(() => {
    setRevealedKey(null);
    setIndex((i) => Math.max(i - 1, 0));
  }, []);

  const reveal = useCallback((key) => setRevealedKey(key), []);
  const hide = useCallback(() => setRevealedKey(null), []);

  return {
    status,
    current: items[index] ?? null,
    index,
    hasPrevious: index > 0,
    hasNext: index < items.length - 1,
    next,
    previous,
    reveal,
    hide,
    isRevealed: (key) => revealedKey === key,
  };
}
