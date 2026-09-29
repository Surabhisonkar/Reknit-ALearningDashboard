package com.learningdashboard.backend.folder;

/** The user already has a folder with this name (compared case-insensitively). Mapped to 409 DUPLICATE_FOLDER_NAME. */
public class DuplicateFolderNameException extends RuntimeException {

    private final String name;

    public DuplicateFolderNameException(String name) {
        super("A folder with this name already exists: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
