package com.learningdashboard.backend.generation.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.learningdashboard.backend.generation.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatHistoryPolicyTest {

    private final ChatHistoryPolicy policy = new ChatHistoryPolicy();

    private List<ChatMessage> chat(int messages, int charsEach) {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < messages; i++) {
            history.add(new ChatMessage(i % 2 == 0 ? "user" : "assistant", ("m" + i + " ").repeat(1) + "x".repeat(charsEach)));
        }
        return history;
    }

    @Test
    void keepsOnlyTheMostRecentMessagesInOrder() {
        List<ChatMessage> trimmed = policy.trim(chat(30, 10));

        assertThat(trimmed).hasSize(ChatHistoryPolicy.MAX_MESSAGES);
        assertThat(trimmed.get(0).text()).startsWith("m18 ");
        assertThat(trimmed.get(trimmed.size() - 1).text()).startsWith("m29 ");
    }

    @Test
    void dropsTheOldestWhenTheCharacterBudgetRunsOut() {
        List<ChatMessage> trimmed = policy.trim(chat(6, 3000));

        assertThat(trimmed).hasSize(2);
        assertThat(trimmed.get(1).text()).startsWith("m5 ");
        assertThat(trimmed.stream().mapToInt(m -> m.text().length()).sum()).isLessThanOrEqualTo(ChatHistoryPolicy.MAX_CHARS);
    }

    @Test
    void emptyOrMissingHistoryIsEmpty() {
        assertThat(policy.trim(null)).isEmpty();
        assertThat(policy.trim(List.of())).isEmpty();
    }
}
