import { useCallback, useState } from "react";

import { saveConcept } from "../../../api/conceptsApi.js";
import { mapConcept } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

/**
 * Confirm-save for a Capture draft. Returns a small outcome object rather
 * than throwing for the expected duplicate-title case, so the page can
 * open its modal without try/catch plumbing:
 *   { outcome: "saved", concept }
 *   { outcome: "duplicate", duplicateConceptId, title }
 *   { outcome: "error" }   (message in `error`)
 */
export function useSaveDraft() {
  const { accessToken } = useAuth();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const save = useCallback(
    async (jobId, { title, onDuplicate } = {}) => {
      setSaving(true);
      setError("");
      try {
        const json = await saveConcept(accessToken, { jobId, title, onDuplicate });
        return { outcome: "saved", concept: mapConcept(json) };
      } catch (err) {
        if (err.status === 409 && err.body?.code === "DUPLICATE_TITLE") {
          return { outcome: "duplicate", duplicateConceptId: err.body.duplicateConceptId, title: err.body.title };
        }
        setError(err.message ?? "Couldn't save this. Please try again.");
        return { outcome: "error" };
      } finally {
        setSaving(false);
      }
    },
    [accessToken]
  );

  const clearError = useCallback(() => setError(""), []);

  return { saving, error, save, clearError };
}
