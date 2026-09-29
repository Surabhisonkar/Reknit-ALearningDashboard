package com.learningdashboard.backend.concept;

import java.util.UUID;

/**
 * Published by the concept module for the folder module: bulk operations on
 * the concepts inside one folder, used when a folder is deleted together
 * with its contents.
 */
public interface ConceptFolderContents {

    /** Deletes every one of the user's concepts filed in this folder; returns how many. Joins the caller's transaction. */
    int deleteAllInFolder(UUID userId, UUID folderId);
}
