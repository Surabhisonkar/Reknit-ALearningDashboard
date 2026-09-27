import { useCallback, useEffect, useRef, useState } from "react";

import { listConceptVersions, getConceptVersion } from "../../../api/conceptsApi.js";
import { mapConceptVersion, mapVersionSummaries } from "../../../domain/conceptMapper.js";
import { useAuth } from "../../../auth";

/**
 * Version history for one concept, for the version dots.
 * - Only fetches the list when the concept actually has more than one
 *   version (the dots are hidden otherwise, so there's nothing to show).
 * - A historical version's full payload is fetched lazily, only when its
 *   dot is selected. Selecting the current version shows the concept itself.
 *
 * @param {string} conceptId
 * @param {number} currentVersion the concept's current version (from useConcept)
 */
export function useConceptVersions(conceptId, currentVersion) {
  const { accessToken } = useAuth();
  const [fetched, setFetched] = useState({ forVersion: 0, list: [] });
  const [selected, setSelected] = useState(null); // { version, data } | null => current
  const [loadingVersion, setLoadingVersion] = useState(null);
  const [error, setError] = useState("");
  const latestRequest = useRef(0);

  const hasHistory = currentVersion > 1;

  useEffect(() => {
    if (!hasHistory) return undefined;
    let cancelled = false;
    listConceptVersions(accessToken, conceptId)
      .then((json) => {
        if (!cancelled) setFetched({ forVersion: currentVersion, list: mapVersionSummaries(json) });
      })
      .catch(() => {
        if (!cancelled) setFetched({ forVersion: currentVersion, list: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, conceptId, currentVersion, hasHistory]);

  // Derived, not stored: a list fetched for an older currentVersion is stale.
  const versions = hasHistory && fetched.forVersion === currentVersion ? fetched.list : [];
  const selectedVersion = selected && selected.version !== currentVersion ? selected.version : currentVersion;

  const select = useCallback(
    async (version) => {
      setError("");
      if (version === currentVersion) {
        latestRequest.current += 1; // ignore any in-flight fetch for another version
        setSelected(null);
        setLoadingVersion(null);
        return;
      }
      const requestId = ++latestRequest.current;
      setLoadingVersion(version);
      try {
        const json = await getConceptVersion(accessToken, conceptId, version);
        if (requestId !== latestRequest.current) return;
        setSelected({ version, data: mapConceptVersion(json) });
      } catch (err) {
        if (requestId !== latestRequest.current) return;
        setError(err.message ?? "Couldn't load that version.");
      } finally {
        if (requestId === latestRequest.current) setLoadingVersion(null);
      }
    },
    [accessToken, conceptId, currentVersion]
  );

  const showCurrent = useCallback(() => {
    latestRequest.current += 1;
    setSelected(null);
    setLoadingVersion(null);
  }, []);

  return {
    versions,
    selectedVersion,
    /** The historical version being viewed, or null when viewing the current one. */
    historicalVersion: selected && selected.version !== currentVersion ? selected.data : null,
    loadingVersion,
    error,
    select,
    showCurrent,
  };
}
