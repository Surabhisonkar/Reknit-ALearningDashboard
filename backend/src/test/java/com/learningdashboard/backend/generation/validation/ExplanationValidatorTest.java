package com.learningdashboard.backend.generation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import org.junit.jupiter.api.Test;

class ExplanationValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExplanationValidator validator = new ExplanationValidator();

    @Test
    void acceptsAValidExplanationWithAnalogyAndExample() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "How TCP Handshakes Work",
                  "overview": "TCP confirms both sides are ready before sending real data.",
                  "sections": [
                    { "heading": "The handshake", "type": "concept",
                      "body": "Client and server exchange three messages to agree they're both listening.",
                      "bullets": [] },
                    { "heading": "Like answering the phone", "type": "analogy", "body": "",
                      "bullets": ["You say hello", "They say hello back", "You confirm you can hear them"] },
                    { "heading": "Loading a webpage", "type": "example",
                      "body": "Your browser handshakes with the server before requesting the page.",
                      "bullets": [] }
                  ]
                }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsExplanationMissingAnAnalogySection() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "X", "overview": "Y",
                  "sections": [
                    { "heading": "A", "type": "concept", "body": "Some body text here.", "bullets": [] },
                    { "heading": "B", "type": "example", "body": "Some body text here.", "bullets": [] }
                  ]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(GenerationException.class)
                .extracting(e -> ((GenerationException) e).getCode())
                .isEqualTo(GenerationException.Code.SCHEMA_VALIDATION_FAILED);
    }

    @Test
    void rejectsExplanationMissingAnExampleSection() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "X", "overview": "Y",
                  "sections": [
                    { "heading": "A", "type": "concept", "body": "Some body text here.", "bullets": [] },
                    { "heading": "B", "type": "analogy", "body": "Some body text here.", "bullets": [] }
                  ]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsSectionWithNeitherBodyNorBullets() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "X", "overview": "Y",
                  "sections": [
                    { "heading": "A", "type": "analogy", "body": "", "bullets": [] },
                    { "heading": "B", "type": "example", "body": "Some body text here.", "bullets": [] }
                  ]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsTooFewSections() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "suggestedTitle": "X", "overview": "Y",
                  "sections": [{ "heading": "A", "type": "analogy", "body": "Some text.", "bullets": [] }] }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsUnknownSectionType() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "X", "overview": "Y",
                  "sections": [
                    { "heading": "A", "type": "diagram", "body": "Some body text.", "bullets": [] },
                    { "heading": "B", "type": "example", "body": "Some body text.", "bullets": [] }
                  ]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsMissingOverview() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "suggestedTitle": "X",
                  "sections": [
                    { "heading": "A", "type": "analogy", "body": "Some body text.", "bullets": [] },
                    { "heading": "B", "type": "example", "body": "Some body text.", "bullets": [] }
                  ]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }
}
