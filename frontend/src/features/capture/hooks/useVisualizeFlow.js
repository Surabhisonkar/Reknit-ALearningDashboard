import { useCallback, useRef, useState } from "react";

import { useAsyncJob } from "../../../shared/hooks/useAsyncJob.js";
import { submitVisualizeJob, pollJob } from "../../../api/jobsApi.js";
import { mapDraft } from "../../../domain/draftMapper.js";
import { useAuth } from "../../../auth";

/**
 * Owns Capture's Visualize step: submits a job, polls it, and holds the
 * resulting *draft* - nothing is saved until the user confirms (see
 * useSaveDraft). Also owns the pre-save "try again": `regenerate()`
 * resubmits the exact same input (including the requested visual type) and replaces the draft in place (the
 * previous draft stays on screen until the new one arrives). `discard()`
 * just drops local state - the abandoned draft's generated assets expire
 * on the server by themselves.
 */
export function useVisualizeFlow() {
  const { accessToken } = useAuth();
  const { status, error, run, reset } = useAsyncJob();
  const [draft, setDraft] = useState(null);
  const lastInput = useRef(null);

  const submit = useCallback(
    (input) =>
      run(async () => {
        const job = await submitVisualizeJob(accessToken, {
          conceptText: input.conceptText,
          preferredVisualizationType: input.preferredType ?? "auto",
          sourceExplainJobId: input.sourceExplainJobId ?? null,
        });
        const completed = await pollJob(accessToken, job.id);
        const mapped = mapDraft(completed);
        setDraft(mapped);
        return mapped;
      }),
    [accessToken, run]
  );

  const visualize = useCallback(
    (conceptText, { sourceExplainJobId, preferredType } = {}) => {
      lastInput.current = { conceptText, sourceExplainJobId, preferredType };
      return submit(lastInput.current);
    },
    [submit]
  );

  const regenerate = useCallback(
    () => (lastInput.current ? submit(lastInput.current) : Promise.resolve(null)),
    [submit]
  );

  const discard = useCallback(() => {
    setDraft(null);
    reset();
  }, [reset]);

  return { status, error, draft, visualize, regenerate, discard, reset };
}
