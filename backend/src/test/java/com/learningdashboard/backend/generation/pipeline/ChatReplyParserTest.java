package com.learningdashboard.backend.generation.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.config.ContentSafetyProperties;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatReplyParserTest {

    private final ChatReplyParser parser = parser(List.of());

    private static ChatReplyParser parser(List<String> disallowed) {
        ContentSafetyProperties props = new ContentSafetyProperties();
        props.setDisallowedTerms(disallowed);
        return new ChatReplyParser(new ObjectMapper(), new ContentSafetyValidator(props));
    }

    @Test
    void readsTheFieldAndTrimsIt() {
        assertThat(parser.readField("{\"answer\": \"  Short answer.  \"}", "answer", 100)).isEqualTo("Short answer.");
    }

    @Test
    void acceptsAReplyWrappedInAMarkdownFence() {
        assertThat(parser.readField("```json\n{\"note\": \"Line one\"}\n```", "note", 100)).isEqualTo("Line one");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.readField("not json", "answer", 100))
                .isInstanceOf(GenerationException.class)
                .extracting("code").isEqualTo(GenerationException.Code.INVALID_JSON);
    }

    @Test
    void rejectsAMissingOrBlankField() {
        assertThatThrownBy(() -> parser.readField("{\"other\": \"x\"}", "answer", 100))
                .extracting("code").isEqualTo(GenerationException.Code.SCHEMA_VALIDATION_FAILED);
        assertThatThrownBy(() -> parser.readField("{\"answer\": \"   \"}", "answer", 100))
                .extracting("code").isEqualTo(GenerationException.Code.SCHEMA_VALIDATION_FAILED);
    }

    @Test
    void rejectsAnAnswerThatIsTooLong() {
        assertThatThrownBy(() -> parser.readField("{\"answer\": \"" + "x".repeat(101) + "\"}", "answer", 100))
                .extracting("code").isEqualTo(GenerationException.Code.SCHEMA_VALIDATION_FAILED);
    }

    @Test
    void runsTheSameContentSafetyCheckAsOtherAiOutput() {
        assertThatThrownBy(() -> parser.readField("{\"answer\": \"<script>alert(1)</script>\"}", "answer", 100))
                .extracting("code").isEqualTo(GenerationException.Code.CONTENT_SAFETY_REJECTED);
        assertThatThrownBy(() -> parser(List.of("forbidden")).readField("{\"answer\": \"a FORBIDDEN word\"}", "answer", 100))
                .extracting("code").isEqualTo(GenerationException.Code.CONTENT_SAFETY_REJECTED);
    }
}
