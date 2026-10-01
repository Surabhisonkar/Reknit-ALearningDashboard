package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.generation.model.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** POST /api/concepts/{id}/notes/from-chat: the chat to condense into a note. */
public class ChatNoteRequest {

    @NotEmpty(message = "history must not be empty")
    @Size(max = 200, message = "history must be at most 200 messages")
    private List<@Valid ChatMessageDto> history = new ArrayList<>();

    public List<ChatMessageDto> getHistory() { return history; }
    public void setHistory(List<ChatMessageDto> history) { this.history = history; }

    public List<ChatMessage> historyMessages() {
        return history.stream().map(ChatMessageDto::toMessage).toList();
    }
}
