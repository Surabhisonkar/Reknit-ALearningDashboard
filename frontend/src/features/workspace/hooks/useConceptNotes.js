import { useCallback, useEffect, useState } from "react";

import { deleteConceptNote, listConceptNotes } from "../../../api/conceptNotesApi.js";
import { useAuth } from "../../../auth";

/** Notes saved on a concept (e.g. condensed Spark chats), with delete. */
export function useConceptNotes(conceptId) {
  const { accessToken } = useAuth();
  const [notes, setNotes] = useState([]);

  useEffect(() => {
    if (!conceptId) return undefined;
    let cancelled = false;
    listConceptNotes(accessToken, conceptId)
      .then((json) => !cancelled && setNotes(json ?? []))
      .catch(() => !cancelled && setNotes([]));
    return () => {
      cancelled = true;
    };
  }, [accessToken, conceptId]);

  const remove = useCallback(
    async (noteId) => {
      await deleteConceptNote(accessToken, conceptId, noteId);
      setNotes((prev) => prev.filter((n) => n.id !== noteId));
    },
    [accessToken, conceptId],
  );

  return { notes, remove };
}
