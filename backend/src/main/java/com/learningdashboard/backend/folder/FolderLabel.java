package com.learningdashboard.backend.folder;

import java.util.UUID;

/** The little a concept response needs to show its folder: id, name and colour. */
public record FolderLabel(UUID id, String name, FolderColor color) {

    static FolderLabel of(Folder folder) {
        return new FolderLabel(folder.getId(), folder.getName(), folder.getColor());
    }
}
