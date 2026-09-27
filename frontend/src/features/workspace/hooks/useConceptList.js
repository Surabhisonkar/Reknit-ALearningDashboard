import { useEffect, useState } from "react";

import { listConcepts, deleteConcept } from "../../../api/conceptsApi.js";
import { mapConceptList } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

/** Fetches the current user's saved concepts and exposes an optimistic remove(). */
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

  return { concepts, status, remove };
}
