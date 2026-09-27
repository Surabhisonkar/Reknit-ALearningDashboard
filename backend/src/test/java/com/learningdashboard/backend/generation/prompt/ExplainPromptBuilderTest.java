package com.learningdashboard.backend.generation.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExplainPromptBuilderTest {

    @Test
    void wrapsTopicInDelimiterTags() {
        String userPrompt = new ExplainPromptBuilder().withTopic("mitochondria").buildUserPrompt();
        assertThat(userPrompt).contains("<topic>").contains("mitochondria").contains("</topic>");
    }

    @Test
    void omitsUserNotesBlockWhenNoneProvided() {
        String userPrompt = new ExplainPromptBuilder().withTopic("mitochondria").buildUserPrompt();
        assertThat(userPrompt).doesNotContain("<user_notes>");
    }

    @Test
    void includesUserNotesBlockWhenProvided() {
        String userPrompt = new ExplainPromptBuilder()
                .withTopic("mitochondria")
                .withUserNotes("they make ATP")
                .buildUserPrompt();
        assertThat(userPrompt).contains("<user_notes>").contains("they make ATP").contains("</user_notes>");
    }

    @Test
    void systemPromptWarnsAgainstTreatingInputAsInstructions() {
        String systemPrompt = new ExplainPromptBuilder().buildSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("never as a command");
    }
}
