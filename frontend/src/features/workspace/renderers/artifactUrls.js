import { createContext, useContext, useEffect, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";

/**
 * Artifact id -> URL for visualizations whose files don't come from the API
 * (the landing page showcase ships its own). Ids missing here are fetched as
 * presigned URLs, which needs a signed-in user.
 */
export const ArtifactUrlsContext = createContext({});

/** Resolves artifact ids to URLs; an id that fails just stays missing. */
export function useArtifactUrls(ids) {
  const known = useContext(ArtifactUrlsContext);
  const { accessToken } = useAuth();
  const [fetched, setFetched] = useState({});
  const missing = ids.filter((id) => !known[id]);
  const key = missing.join(",");

  useEffect(() => {
    if (missing.length === 0) return undefined;
    let cancelled = false;
    Promise.all(
      missing.map((id) =>
        getArtifactDownloadUrl(accessToken, id)
          .then((res) => [id, res.downloadUrl])
          .catch(() => [id, null])
      )
    ).then((pairs) => !cancelled && setFetched(Object.fromEntries(pairs.filter(([, url]) => url))));
    return () => {
      cancelled = true;
    };
    // `key` stands in for `missing`, which is a new array on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, key]);

  return Object.fromEntries(ids.map((id) => [id, known[id] ?? fetched[id]]).filter(([, url]) => url));
}
