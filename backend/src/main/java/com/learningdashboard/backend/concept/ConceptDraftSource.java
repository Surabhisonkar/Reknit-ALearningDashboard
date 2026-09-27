package com.learningdashboard.backend.concept;

import java.util.UUID;

/**
 * Port: where unsaved drafts come from. The concept module declares it;
 * the generation module implements it (drafts currently live on the
 * {@code generation_jobs.result_payload} of a completed Visualize job).
 * Keeping it an interface is what lets {@link ConceptSaveService} be
 * tested - and later re-homed - without knowing jobs exist at all.
 */
public interface ConceptDraftSource {

    /**
     * Loads the caller's draft and locks it for the rest of the current
     * transaction, so two concurrent saves of the same draft serialize
     * instead of both creating a concept.
     *
     * @throws com.learningdashboard.backend.common.exception.NotFoundException if the job doesn't exist or isn't the user's
     * @throws DraftNotSaveableException if the job exists but isn't a saveable draft (not finished, failed, expired, a regenerate)
     */
    DraftClaim claim(UUID draftId, UUID userId);

    /** Records that the draft has been saved as {@code conceptId}. Must be called inside the same transaction as {@link #claim}. */
    void markSaved(UUID draftId, UUID conceptId);
}
