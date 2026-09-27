import { useCallback, useEffect, useRef, useState } from "react";

import { getSparkFeed, getConceptFolders } from "../../../api/conceptsApi.js";
import { mapConceptList } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

const PAGE_SIZE = 8;
const GAME_EVERY = 3; // a mini-game slot after every 3 animation cards

/** A feed item is either a saved concept or a game slot interleaved between them. */
function interleaveGames(existingItems, newConcepts) {
  const items = [...existingItems];
  let sinceLastGame = items.filter((item) => item.kind === "concept").length % GAME_EVERY;

  for (const concept of newConcepts) {
    items.push({ kind: "concept", key: concept.id, concept });
    sinceLastGame += 1;
    if (sinceLastGame >= GAME_EVERY) {
      items.push({ kind: "game", key: `game-${concept.id}` });
      sinceLastGame = 0;
    }
  }
  return items;
}

/**
 * Owns Spark's whole data story: the folder picker's options, the
 * random/folder-scoped source toggle, and paging through the feed
 * without repeating a card already shown this session. Pages compose
 * this rather than talking to the API directly.
 */
export function useSparkFeed() {
  const { accessToken } = useAuth();
  const [folders, setFolders] = useState([]);
  const [source, setSource] = useState("all"); // "all" | a folder name
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState("loading"); // loading | ready | loading-more | empty | error
  const seenIds = useRef(new Set());

  const load = useCallback(
    async (folder, { append } = { append: false }) => {
      setStatus(append ? "loading-more" : "loading");
      try {
        const excludeIds = append ? Array.from(seenIds.current) : [];
        const json = await getSparkFeed(accessToken, { folder: folder || undefined, excludeIds, limit: PAGE_SIZE });
        const mapped = mapConceptList(json);
        mapped.forEach((c) => seenIds.current.add(c.id));

        setItems((prev) => interleaveGames(append ? prev : [], mapped));

        if (mapped.length === 0 && !append) {
          setStatus("empty");
        } else {
          setStatus("ready");
        }
      } catch {
        setStatus("error");
      }
    },
    [accessToken]
  );

  // Initial load, and reload from scratch whenever the source changes.
  // load() itself always fully replaces items on a non-append call (see
  // interleaveGames above), so there's no separate setItems([]) here.
  // The fetch is kicked off from inside a microtask callback, not as a
  // direct statement in the effect body, matching how useConceptList's
  // effect only ever sets state from inside a .then() callback.
  useEffect(() => {
    seenIds.current = new Set();
    let cancelled = false;
    queueMicrotask(() => {
      if (!cancelled) load(source === "all" ? null : source, { append: false });
    });
    return () => {
      cancelled = true;
    };
  }, [source, load]);

  useEffect(() => {
    getConceptFolders(accessToken)
      .then(setFolders)
      .catch(() => setFolders([]));
  }, [accessToken]);

  const loadMore = useCallback(() => {
    if (status === "loading-more") return;
    load(source === "all" ? null : source, { append: true });
  }, [load, source, status]);

  return { folders, source, setSource, items, status, loadMore };
}
