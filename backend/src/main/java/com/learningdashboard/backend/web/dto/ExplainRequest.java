package com.learningdashboard.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExplainRequest {

    @NotBlank(message = "topic must not be blank")
    @Size(max = 500, message = "topic must be at most 500 characters")
    private String topic;

    /** Optional - the user's own rough notes, if they provided any alongside the topic. */
    @Size(max = 4000, message = "userNotes must be at most 4000 characters")
    private String userNotes;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getUserNotes() { return userNotes; }
    public void setUserNotes(String userNotes) { this.userNotes = userNotes; }
}
