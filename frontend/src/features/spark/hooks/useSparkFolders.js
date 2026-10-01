import { useEffect, useState } from "react";

import { getConceptFolders } from "../../../api/conceptsApi.js";

/** Names of the user's non-empty folders, for Folder mode's picker. */
export function useSparkFolders(accessToken) {
  const [folders, setFolders] = useState([]);
  useEffect(() => {
    let cancelled = false;
    getConceptFolders(accessToken)
      .then((names) => !cancelled && setFolders(names))
      .catch(() => !cancelled && setFolders([]));
    return () => {
      cancelled = true;
    };
  }, [accessToken]);
  return folders;
}
