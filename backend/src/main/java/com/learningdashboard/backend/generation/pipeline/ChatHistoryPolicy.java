package com.learningdashboard.backend.generation.pipeline;

import com.learningdashboard.backend.generation.model.ChatMessage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chats have no length cap (user decision), but a prompt must stay bounded:
 * only the most recent messages that fit are sent to the model - at most
 * {@link #MAX_MESSAGES} and {@link #MAX_CHARS} characters. The oldest go first.
 */
@Component
public class ChatHistoryPolicy {

    public static final int MAX_MESSAGES = 12;
    public static final int MAX_CHARS = 8000;

    public List<ChatMessage> trim(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        Deque<ChatMessage> kept = new ArrayDeque<>();
        int chars = 0;
        for (int i = history.size() - 1; i >= 0 && kept.size() < MAX_MESSAGES; i--) {
            ChatMessage message = history.get(i);
            int length = message.text() == null ? 0 : message.text().length();
            if (chars + length > MAX_CHARS) {
                break;
            }
            kept.addFirst(message);
            chars += length;
        }
        return List.copyOf(kept);
    }
}
