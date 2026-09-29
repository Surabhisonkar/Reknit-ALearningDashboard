package com.learningdashboard.backend.folder;

/** What happens to a folder's concepts when the folder is deleted. */
public enum FolderDeletionMode {
    /** The concepts stay and become unfiled (the database sets their folder_id to NULL). */
    UNFILE,
    /** The concepts are deleted together with the folder, in the same transaction. */
    DELETE_CONCEPTS;

    /**
     * Parses the API's required {@code ?concepts=} value: "unfile" or "delete".
     * Anything else - including a missing value - is a 400, so a folder's
     * concepts can never be deleted by an omitted parameter.
     */
    public static FolderDeletionMode fromQueryValue(String value) {
        if ("unfile".equals(value)) {
            return UNFILE;
        }
        if ("delete".equals(value)) {
            return DELETE_CONCEPTS;
        }
        throw new InvalidFolderRequestException("Query parameter 'concepts' must be 'unfile' or 'delete'.");
    }
}
