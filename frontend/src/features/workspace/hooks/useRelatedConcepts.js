import { useEffect, useState } from "react";

import { getRelatedConcepts } from "../../../api/relatedConceptsApi.js";
import { mapRelatedConcepts } from "../../../domain/relatedConceptMapper.js";
import { useAuth } from "../../../auth";

/**
 * Loads a concept's related concepts. A failure is treated like "none" -
 * the panel is a bonus and should never put an error in front of the
 * user's visualization.
 *
 * @param {string} conceptId
 * @param {number} refreshKey pass the concept's currentVersion: a regenerate can change title/summary
 */
export function useRelatedConcepts(conceptId, refreshKey) {
  const { accessToken } = useAuth();
  const [state, setState] = useState({ key: null, related: [] });
  const requestKey = `${conceptId}:${refreshKey}`;

  useEffect(() => {
    let cancelled = false;
    getRelatedConcepts(accessToken, conceptId)
      .then((json) => {
        if (!cancelled) setState({ key: requestKey, related: mapRelatedConcepts(json) });
      })
      .catch(() => {
        if (!cancelled) setState({ key: requestKey, related: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, conceptId, requestKey]);

  // Until the fetch for *this* concept/version lands, report nothing (never stale cards from another concept).
  const related = state.key === requestKey ? state.related : [];
  return { related, loaded: state.key === requestKey };
}
