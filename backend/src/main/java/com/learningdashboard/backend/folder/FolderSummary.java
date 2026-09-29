package com.learningdashboard.backend.folder;

/** A folder plus how many concepts it holds - one row of the Library's folder list. */
public record FolderSummary(Folder folder, long conceptCount) {
}
