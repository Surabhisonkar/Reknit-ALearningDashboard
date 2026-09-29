package com.learningdashboard.backend.concept;

import java.util.Map;
import java.util.UUID;

/**
 * Published by the concept module for the folder module: how many concepts
 * each folder holds. Lets the folder module show counts without reading
 * the concepts table itself.
 */
public interface ConceptFolderStats {

    /** folderId -> number of the user's concepts filed there. Unfiled concepts and empty folders are absent. */
    Map<UUID, Long> countByFolder(UUID userId);
}
