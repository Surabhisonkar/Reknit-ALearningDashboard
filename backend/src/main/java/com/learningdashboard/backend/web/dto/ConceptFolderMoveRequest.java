package com.learningdashboard.backend.web.dto;

import java.util.UUID;

/** {@code folderId: null} (or omitted) unfiles the concept. */
public class ConceptFolderMoveRequest {

    private UUID folderId;

    public UUID getFolderId() { return folderId; }
    public void setFolderId(UUID folderId) { this.folderId = folderId; }
}
