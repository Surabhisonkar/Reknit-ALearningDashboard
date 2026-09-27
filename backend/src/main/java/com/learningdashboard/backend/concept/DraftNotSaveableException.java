package com.learningdashboard.backend.concept;

/** The referenced job exists and is the caller's, but can't be saved as a concept right now. */
public class DraftNotSaveableException extends RuntimeException {

    public enum Reason {
        /** Still running, failed, or not a new-concept Visualize job. */
        NOT_READY,
        /** Older than the draft lifetime - its generated assets may already be gone. */
        EXPIRED
    }

    private final Reason reason;

    public DraftNotSaveableException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
