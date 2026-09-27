package com.learningdashboard.backend.concept;

import java.util.UUID;

/** Result of a post-save regenerate. {@code duplicateTitleConceptId} is informational only (another concept now shares the new title). */
public record VersionAppended(UUID conceptId, int version, UUID duplicateTitleConceptId) { }
