import { useCallback, useEffect, useState } from "react";

import { getConcept, deleteConcept } from "../../../api/conceptsApi.js";
import { mapConcept } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

/** Fetches one concept by id. Expects conceptId to be stable/defined - callers that key a component by conceptId (see WorkspacePage) get correct fresh-state-per-id for free. */
export function useConcept(conceptId) {
  const { accessToken } = useAuth();
  const [concept, setConcept] = useState(null);
  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [errorMessage, setErrorMessage] = useState("");
  // Bumped by reload() to refetch in place (e.g. after a regenerate) without
  // dropping back to the "loading" state, so the page doesn't flash.
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;
    getConcept(accessToken, conceptId)
      .then((json) => {
        if (cancelled) return;
        setConcept(mapConcept(json));
        setStatus("ready");
      })
      .catch((error) => {
        if (cancelled) return;
        setErrorMessage(error.message ?? "Couldn't load this concept.");
        setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, conceptId, reloadToken]);

  async function remove() {
    await deleteConcept(accessToken, conceptId);
  }

  const reload = useCallback(() => setReloadToken((t) => t + 1), []);

  return { concept, status, errorMessage, remove, reload };
}
