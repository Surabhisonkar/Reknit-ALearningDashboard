import { useCallback, useState } from "react";

/**
 * Tracks status/error around any async operation. Deliberately generic
 * (not "submit a job and poll it" specifically) - feature hooks compose
 * this with their own domain logic, which is what keeps this reusable
 * across Explain, Visualize, and any future async-job-backed action
 * without this hook needing to know about jobs at all.
 */
export function useAsyncJob() {
  const [status, setStatus] = useState("idle"); // idle | loading | ready | error
  const [error, setError] = useState("");

  const run = useCallback(async (asyncFn) => {
    setStatus("loading");
    setError("");
    try {
      const result = await asyncFn();
      setStatus("ready");
      return result;
    } catch (err) {
      setStatus("error");
      setError(err.message ?? "Something went wrong. Please try again.");
      throw err;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus("idle");
    setError("");
  }, []);

  return { status, error, run, reset };
}
