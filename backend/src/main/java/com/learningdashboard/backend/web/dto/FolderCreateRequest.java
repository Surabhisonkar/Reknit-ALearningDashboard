package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.folder.Folder;
import com.learningdashboard.backend.folder.FolderColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FolderCreateRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = Folder.MAX_NAME_LENGTH, message = "name must be at most 120 characters")
    private String name;

    /** Optional palette key ("teal"); null lets the server pick the least-used colour. An unknown key is a 400. */
    private FolderColor color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FolderColor getColor() { return color; }
    public void setColor(FolderColor color) { this.color = color; }
}
