package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.folder.Folder;
import com.learningdashboard.backend.folder.FolderColor;
import jakarta.validation.constraints.Size;

/** Rename and/or recolour: omit a field to leave it unchanged. */
public class FolderUpdateRequest {

    @Size(max = Folder.MAX_NAME_LENGTH, message = "name must be at most 120 characters")
    private String name;

    private FolderColor color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FolderColor getColor() { return color; }
    public void setColor(FolderColor color) { this.color = color; }
}
