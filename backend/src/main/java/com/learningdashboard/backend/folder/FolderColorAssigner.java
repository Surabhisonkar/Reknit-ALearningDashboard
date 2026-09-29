package com.learningdashboard.backend.folder;

import java.util.UUID;

/** Strategy for the colour a new folder gets when nobody picked one. */
public interface FolderColorAssigner {

    FolderColor nextColor(UUID userId);
}
