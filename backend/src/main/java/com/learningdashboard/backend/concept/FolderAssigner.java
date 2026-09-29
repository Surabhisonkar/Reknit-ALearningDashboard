package com.learningdashboard.backend.concept;

import java.util.Optional;
import java.util.UUID;

/**
 * Port owned by the concept module: "give me the id of this user's folder
 * with this name, creating it if needed". Used by confirm-save to auto-file
 * a concept under the AI's suggested folder (or the user's override). The
 * concept module never learns how folders are stored.
 */
public interface FolderAssigner {

    /** Empty for a blank name - the concept is then saved unfiled. */
    Optional<UUID> findOrCreate(UUID userId, String folderName);
}
