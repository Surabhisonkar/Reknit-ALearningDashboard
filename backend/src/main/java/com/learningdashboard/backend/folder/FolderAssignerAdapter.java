package com.learningdashboard.backend.folder;

import com.learningdashboard.backend.concept.FolderAssigner;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implements the concept module's {@link FolderAssigner} port. Kept separate
 * from {@link FolderLookupAdapter} on purpose: this one needs
 * {@link FolderService}, which needs the concept module, so one combined
 * adapter would form a constructor cycle at startup.
 */
@Component
public class FolderAssignerAdapter implements FolderAssigner {

    private final FolderService folderService;

    public FolderAssignerAdapter(FolderService folderService) {
        this.folderService = folderService;
    }

    @Override
    public Optional<UUID> findOrCreate(UUID userId, String folderName) {
        return folderService.findOrCreateByName(userId, folderName).map(Folder::getId);
    }
}
