package com.learningdashboard.backend.folder;

import com.learningdashboard.backend.concept.FolderLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the concept module's read-only {@link FolderLookup} port. See {@link FolderAssignerAdapter} for why the two are separate. */
@Component
public class FolderLookupAdapter implements FolderLookup {

    private final FolderQueryService folderQueryService;

    public FolderLookupAdapter(FolderQueryService folderQueryService) {
        this.folderQueryService = folderQueryService;
    }

    @Override
    public Optional<UUID> findIdByName(UUID userId, String folderName) {
        return folderQueryService.findIdByName(userId, folderName);
    }

    @Override
    public boolean isOwnedBy(UUID folderId, UUID userId) {
        return folderQueryService.isOwnedBy(folderId, userId);
    }
}
