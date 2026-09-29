package com.learningdashboard.backend.concept;

import java.util.Optional;
import java.util.UUID;

/** Port owned by the concept module: read-only questions about folders. */
public interface FolderLookup {

    /** Case-insensitive, like folder names themselves. Empty for a blank or unknown name. */
    Optional<UUID> findIdByName(UUID userId, String folderName);

    boolean isOwnedBy(UUID folderId, UUID userId);
}
