import { useCallback, useEffect, useState } from "react";

import { listFolders } from "../../../api/foldersApi.js";
import { mapFolderList } from "../../../domain/folderMapper.js";
import { useAuth } from "../../../auth";

/** Loads the user's folders (with concept counts) and exposes reload() for after a change. */
export function useFolders() {
  const { accessToken } = useAuth();
  const [folders, setFolders] = useState([]);
  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    listFolders(accessToken)
      .then((json) => {
        if (cancelled) return;
        setFolders(mapFolderList(json));
        setStatus("ready");
      })
      .catch(() => !cancelled && setStatus("error"));
    return () => {
      cancelled = true;
    };
  }, [accessToken, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  return { folders, status, reload };
}
