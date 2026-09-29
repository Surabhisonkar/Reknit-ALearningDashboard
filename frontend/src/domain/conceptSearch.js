/**
 * Pure, client-side Library filtering. The whole library is already loaded
 * and a personal library is small, so no server round trip is needed
 * (and no AI call ever happens from the api process for search).
 */

/** "all" | "unfiled" | a folder id. */
export const ALL_FOLDERS = "all";
export const UNFILED = "unfiled";

function normalize(text) {
  return (text ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function inFolder(concept, folderFilter) {
  if (folderFilter === ALL_FOLDERS) return true;
  if (folderFilter === UNFILED) return !concept.folderId;
  return concept.folderId === folderFilter;
}

/**
 * Keeps concepts in the selected folder whose title or summary contains
 * every word of the query. Case- and accent-insensitive for searching only;
 * folder names themselves stay accent-sensitive on the server.
 */
export function filterConcepts(concepts, query, folderFilter = ALL_FOLDERS) {
  const words = normalize(query).split(/\s+/).filter(Boolean);
  return concepts.filter((concept) => {
    if (!inFolder(concept, folderFilter)) return false;
    if (words.length === 0) return true;
    const haystack = `${normalize(concept.title)} ${normalize(concept.summary)}`;
    return words.every((word) => haystack.includes(word));
  });
}
