package com.learningdashboard.backend.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ConceptNoteResponse(UUID id, String content, String source, Instant createdAt) { }
