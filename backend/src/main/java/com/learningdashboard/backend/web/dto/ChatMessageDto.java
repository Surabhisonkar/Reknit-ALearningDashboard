package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.generation.model.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One chat turn as the app sends it. */
public class ChatMessageDto {

    @NotBlank
    @Pattern(regexp = "user|assistant", message = "role must be user or assistant")
    private String role;

    @NotBlank(message = "text must not be blank")
    @Size(max = 4000, message = "text must be at most 4000 characters")
    private String text;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public ChatMessage toMessage() {
        return new ChatMessage(role, text);
    }
}
