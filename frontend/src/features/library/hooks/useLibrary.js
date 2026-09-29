import { useMemo, useState } from "react";

import { createFolder as apiCreateFolder, deleteFolder as apiDeleteFolder, updateFolder as apiUpdateFolder } from "../../../api/foldersApi.js";
import { moveConceptToFolder } from "../../../api/conceptsApi.js";
import { mapConcept } from "../../../domain/conceptMapper.js";
import { ALL_FOLDERS, UNFILED, filterConcepts } from "../../../domain/conceptSearch.js";
import { useAuth } from "../../../auth";
import { useConceptList } from "../../workspace";
import { useFolders } from "./useFolders.js";

/**
 * Everything the Library page needs: concepts, folders, the folder filter,
 * search, and the folder/concept actions. Folder counts come from the server
 * and are reloaded after any change that moves concepts between folders.
 */
export function useLibrary() {
  const { accessToken } = useAuth();
  const { concepts, status: conceptStatus, remove, replaceConcept, forgetConcepts } = useConceptList();
  const { folders, status: folderStatus, reload: reloadFolders } = useFolders();
  const [query, setQuery] = useState("");
  const [activeFolder, setActiveFolder] = useState(ALL_FOLDERS);

  const status =
    conceptStatus === "error" || folderStatus === "error"
      ? "error"
      : conceptStatus === "loading" || folderStatus === "loading"
        ? "loading"
        : "ready";

  const visibleConcepts = useMemo(
    () => filterConcepts(concepts, query, activeFolder),
    [concepts, query, activeFolder],
  );

  const unfiledCount = useMemo(() => concepts.filter((c) => !c.folderId).length, [concepts]);
  const selectedFolder = folders.find((f) => f.id === activeFolder) ?? null;

  async function createFolder(name, color) {
    const created = await apiCreateFolder(accessToken, { name, color });
    reloadFolders();
    return created;
  }

  async function updateFolder(id, changes) {
    await apiUpdateFolder(accessToken, id, changes);
    reloadFolders();
    // Concept cards show their folder's name and colour, so refresh those locally.
    concepts
      .filter((c) => c.folderId === id)
      .forEach((c) =>
        replaceConcept({
          ...c,
          folder: changes.name ?? c.folder,
          folderColor: changes.color ?? c.folderColor,
        }),
      );
  }

  /** deleteConcepts: false moves the folder's concepts to Unfiled; true deletes them with the folder. */
  async function deleteFolder(id, { deleteConcepts = false } = {}) {
    await apiDeleteFolder(accessToken, id, { deleteConcepts });
    const inFolder = concepts.filter((c) => c.folderId === id);
    if (deleteConcepts) {
      forgetConcepts(inFolder.map((c) => c.id));
    } else {
      inFolder.forEach((c) => replaceConcept({ ...c, folder: "", folderId: null, folderColor: null }));
    }
    if (activeFolder === id) setActiveFolder(ALL_FOLDERS);
    reloadFolders();
  }

  async function moveConcept(conceptId, folderId) {
    const json = await moveConceptToFolder(accessToken, conceptId, folderId);
    replaceConcept(mapConcept(json));
    reloadFolders();
  }

  /** Creates a folder and moves the concept into it - used by the move dialog. color null = server picks. */
  async function createFolderAndMove(conceptId, name, color) {
    const created = await apiCreateFolder(accessToken, { name, color });
    await moveConcept(conceptId, created.id);
  }

  async function removeConcept(id) {
    await remove(id);
    reloadFolders();
  }

  return {
    status,
    concepts,
    folders,
    unfiledCount,
    selectedFolder,
    query,
    setQuery,
    activeFolder,
    setActiveFolder,
    visibleConcepts,
    createFolder,
    updateFolder,
    deleteFolder,
    moveConcept,
    createFolderAndMove,
    removeConcept,
  };
}

export { ALL_FOLDERS, UNFILED };
