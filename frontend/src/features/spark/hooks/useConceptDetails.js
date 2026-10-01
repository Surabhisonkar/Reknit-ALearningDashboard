import { useEffect, useState } from "react";

import { getConcept } from "../../../api/conceptsApi.js";
import { mapConcept } from "../../../domain/conceptMapper.js";

/** Loads a full concept (with its visual) only once `enabled` - related cards fetch on reveal. */
export function useConceptDetails(accessToken, conceptId, enabled) {
  const [state, setState] = useState({ concept: null, status: "idle" });

  useEffect(() => {
    if (!enabled || !conceptId) return undefined;
    let cancelled = false;
    getConcept(accessToken, conceptId)
      .then((json) => !cancelled && setState({ concept: mapConcept(json), status: "ready" }))
      .catch(() => !cancelled && setState({ concept: null, status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [accessToken, conceptId, enabled]);

  return enabled && state.status === "idle" ? { concept: null, status: "loading" } : state;
}
