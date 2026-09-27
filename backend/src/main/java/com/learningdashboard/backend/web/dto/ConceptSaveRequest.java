package com.learningdashboard.backend.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code POST /api/concepts} (confirm-save).
 * {@code jobId} is the completed Visualize job whose result is the draft.
 */
public class ConceptSaveRequest {

    @NotNull(message = "jobId is required")
    private UUID jobId;

    /** Optional - saves under this title instead of the draft's (the "rename" duplicate resolution). */
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    /** Optional - overrides the AI's suggested folder. */
    @Size(max = 120, message = "folder must be at most 120 characters")
    private String folder;

    @Pattern(regexp = "REJECT|KEEP_BOTH|REPLACE", message = "onDuplicate must be one of REJECT, KEEP_BOTH, REPLACE")
    private String onDuplicate = "REJECT";

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public String getOnDuplicate() { return onDuplicate; }
    public void setOnDuplicate(String onDuplicate) { this.onDuplicate = onDuplicate == null ? "REJECT" : onDuplicate; }
}
