package com.learningdashboard.backend.concept;

/**
 * @param created false when the draft had already been saved and the existing concept is returned (idempotent retry)
 */
public record SaveOutcome(Concept concept, boolean created) { }
