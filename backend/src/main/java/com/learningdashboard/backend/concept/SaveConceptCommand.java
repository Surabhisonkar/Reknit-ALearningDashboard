package com.learningdashboard.backend.concept;

import java.util.UUID;

/**
 * Input to {@link ConceptSaveService#save}.
 *
 * @param titleOverride  optional - replaces the draft's title (the "rename" duplicate resolution)
 * @param folderOverride optional - replaces the model's suggested folder
 * @param onDuplicate    never null; defaults to {@link DuplicateTitlePolicy#REJECT}
 */
public record SaveConceptCommand(UUID draftId, UUID userId, String titleOverride, String folderOverride,
                                 DuplicateTitlePolicy onDuplicate) {
    public SaveConceptCommand {
        onDuplicate = onDuplicate == null ? DuplicateTitlePolicy.REJECT : onDuplicate;
    }
}
