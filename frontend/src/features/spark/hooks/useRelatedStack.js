import { useCallback, useState } from "react";

import { getRelatedConcepts } from "../../../api/relatedConceptsApi.js";
import { mapRelatedConcepts } from "../../../domain/relatedConceptMapper.js";

/**
 * The horizontal sub-stack of concepts related to one card (swipe left to
 * open / go deeper, swipe right to back out). Uses the related-concepts
 * endpoint from Phase 6.
 */
export function useRelatedStack(accessToken) {
  const [stack, setStack] = useState(null); // { parent, items, index, status } | null

  const open = useCallback(
    (parent) => {
      setStack({ parent, items: [], index: 0, status: "loading" });
      getRelatedConcepts(accessToken, parent.id)
        .then((json) => {
          const items = mapRelatedConcepts(json);
          setStack((s) => (s?.parent.id === parent.id ? { ...s, items, status: items.length ? "ready" : "empty" } : s));
        })
        .catch(() => setStack((s) => (s?.parent.id === parent.id ? { ...s, status: "error" } : s)));
    },
    [accessToken],
  );

  const deeper = useCallback(() => {
    setStack((s) => (s && s.index < s.items.length - 1 ? { ...s, index: s.index + 1 } : s));
  }, []);

  const close = useCallback(() => setStack(null), []);

  return { stack, open, deeper, close };
}
