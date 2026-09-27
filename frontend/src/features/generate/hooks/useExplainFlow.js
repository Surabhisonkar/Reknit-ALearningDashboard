import { useCallback, useState } from "react";

import { useAsyncJob } from "../../../shared/hooks/useAsyncJob.js";
import { submitExplainJob, pollJob } from "../../../api/jobsApi.js";
import { mapExplanation } from "../../../domain/explanationMapper.js";
import { useAuth } from "../../../auth";

/** Owns the Explain flow's state so pages only ever compose this hook, never manage the status/error/result bookkeeping themselves. */
export function useExplainFlow() {
  const { accessToken } = useAuth();
  const { status, error, run, reset } = useAsyncJob();
  const [explanation, setExplanation] = useState(null);
  const [jobId, setJobId] = useState(null);
  const [useAiExplanation, setUseAiExplanation] = useState(true);

  const explain = useCallback(
    (topic, userNotes) =>
      run(async () => {
        const job = await submitExplainJob(accessToken, { topic, userNotes: userNotes || null });
        setJobId(job.id);
        const completed = await pollJob(accessToken, job.id);
        const mapped = mapExplanation(completed.resultPayload);
        setExplanation(mapped);
        setUseAiExplanation(true);
        return mapped;
      }),
    [accessToken, run]
  );

  return { status, error, explanation, jobId, useAiExplanation, setUseAiExplanation, explain, reset };
}
