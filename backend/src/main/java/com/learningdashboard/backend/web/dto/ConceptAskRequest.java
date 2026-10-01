package com.learningdashboard.backend.web.dto;

import com.learningdashboard.backend.generation.model.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/concepts/{id}/ask. Chats have no length cap (user decision); the
 * 200-message bound here only limits one request's size - the prompt itself
 * uses just the most recent messages (ChatHistoryPolicy).
 */
public class ConceptAskRequest {

    @NotNull
    @Size(max = 200, message = "history must be at most 200 messages")
    private List<@Valid ChatMessageDto> history = new ArrayList<>();

    @NotBlank(message = "question must not be blank")
    @Size(max = 1000, message = "question must be at most 1000 characters")
    private String question;

    public List<ChatMessageDto> getHistory() { return history; }
    public void setHistory(List<ChatMessageDto> history) { this.history = history; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<ChatMessage> historyMessages() {
        return history.stream().map(ChatMessageDto::toMessage).toList();
    }
}
