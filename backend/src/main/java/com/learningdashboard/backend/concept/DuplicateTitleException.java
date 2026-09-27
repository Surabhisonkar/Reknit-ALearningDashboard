package com.learningdashboard.backend.concept;

import java.util.UUID;

/** Confirm-save refused because of a title collision under {@link DuplicateTitlePolicy#REJECT}. Nothing was written. */
public class DuplicateTitleException extends RuntimeException {

    private final UUID duplicateConceptId;
    private final String title;

    public DuplicateTitleException(UUID duplicateConceptId, String title) {
        super("A concept titled '" + title + "' already exists.");
        this.duplicateConceptId = duplicateConceptId;
        this.title = title;
    }

    public UUID getDuplicateConceptId() { return duplicateConceptId; }
    public String getTitle() { return title; }
}
