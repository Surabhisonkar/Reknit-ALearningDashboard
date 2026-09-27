package com.learningdashboard.backend.concept;

/** What confirm-save does when the draft's title matches one of the user's existing concepts (case-insensitive). */
public enum DuplicateTitlePolicy {
    /** Default: write nothing, report the collision so the client can ask the user. */
    REJECT,
    /** Save anyway; two concepts share a title. */
    KEEP_BOTH,
    /** Save the new concept, then delete the colliding old one - same transaction. */
    REPLACE
}
