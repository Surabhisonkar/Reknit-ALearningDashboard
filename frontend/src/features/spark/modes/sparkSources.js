import { getSparkFeed } from "../../../api/conceptsApi.js";
import { mapConceptList } from "../../../domain/conceptMapper.js";

/**
 * Feed sources. Each has nextPage(excludeIds, limit) -> Promise<Concept[]>.
 * The server decides the mix of visual types (see config/feedMix.js).
 */
export function createShuffleSource(accessToken) {
  return {
    nextPage: (excludeIds, limit) => getSparkFeed(accessToken, { excludeIds, limit }).then(mapConceptList),
  };
}

export function createFolderSource(accessToken, folderName) {
  return {
    nextPage: (excludeIds, limit) =>
      getSparkFeed(accessToken, { folder: folderName, excludeIds, limit }).then(mapConceptList),
  };
}
