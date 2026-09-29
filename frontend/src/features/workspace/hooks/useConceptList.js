import { useEffect, useState } from "react";

import { listConcepts, deleteConcept } from "../../../api/conceptsApi.js";
import { mapConceptList } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

/** Fetches the current user's saved concepts and exposes an optimistic remove(), replaceConcept() and forgetConcepts(). */
export function useConceptList() {
  const { accessToken } = useAuth();
  const [concepts, setConcepts] = useState([]);
  const [status, setStatus] = useState("loading"); // loading | ready | error

  useEffect(() => {
    let cancelled = false;
    listConcepts(accessToken)
      .then((json) => {
        if (cancelled) return;
        setConcepts(mapConceptList(json));
        setStatus("ready");
      })
      .catch(() => !cancelled && setStatus("error"));
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  async function remove(id) {
    await deleteConcept(accessToken, id);
    setConcepts((prev) => prev.filter((c) => c.id !== id));
  }

  /** Swaps in an updated copy of one concept (e.g. after a folder move) without refetching the list. */
  function replaceConcept(updated) {
    setConcepts((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
  }

  /** Drops concepts the server has already deleted (e.g. with their folder) from the local list, without another request. */
  function forgetConcepts(ids) {
    const gone = new Set(ids);
    setConcepts((prev) => prev.filter((c) => !gone.has(c.id)));
  }

  return { concepts, status, remove, replaceConcept, forgetConcepts };
}
