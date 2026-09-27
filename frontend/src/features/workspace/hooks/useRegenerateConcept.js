import { useCallback } from "react";

import { useAsyncJob } from "../../../shared/hooks/useAsyncJob.js";
import { submitRegenerateJob, pollJob } from "../../../api/jobsApi.js";
import { useAuth } from "../../../auth";

/**
 * Post-save regenerate: asks the server for a new version of an already
 * saved concept and waits for it. The server reuses the concept's
 * original input and appends version N+1 - older versions are kept.
 * Resolves with the completed job ({ resultPayload: { version } }).
 */
export function useRegenerateConcept(conceptId) {
  const { accessToken } = useAuth();
  const { status, error, run, reset } = useAsyncJob();

  const regenerate = useCallback(
    () =>
      run(async () => {
        const job = await submitRegenerateJob(accessToken, conceptId);
        return pollJob(accessToken, job.id);
      }),
    [accessToken, conceptId, run]
  );

  return { status, error, regenerate, reset };
}
