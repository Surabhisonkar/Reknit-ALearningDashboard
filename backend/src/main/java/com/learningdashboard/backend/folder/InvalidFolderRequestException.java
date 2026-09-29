package com.learningdashboard.backend.folder;

/** A folder request that is well-formed JSON but not acceptable (blank name, bad deletion mode). Mapped to 400. */
public class InvalidFolderRequestException extends RuntimeException {

    public InvalidFolderRequestException(String message) {
        super(message);
    }
}
