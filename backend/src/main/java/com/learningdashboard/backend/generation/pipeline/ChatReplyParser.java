package com.learningdashboard.backend.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import org.springframework.stereotype.Component;

/**
 * Reads the one text field a chat prompt asks for ({"answer": ...} or
 * {"note": ...}): the reply must be JSON, the field non-blank and within its
 * length, and the text must pass the same content-safety check as every
 * other AI output.
 */
@Component
public class ChatReplyParser {

    private final ObjectMapper objectMapper;
    private final ContentSafetyValidator contentSafetyValidator;

    public ChatReplyParser(ObjectMapper objectMapper, ContentSafetyValidator contentSafetyValidator) {
        this.objectMapper = objectMapper;
        this.contentSafetyValidator = contentSafetyValidator;
    }

    public String readField(String rawResponse, String field, int maxLength) {
        JsonNode parsed;
        try {
            String cleaned = rawResponse.trim().replaceFirst("^```(json)?\\n?", "").replaceFirst("```$", "").trim();
            parsed = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new GenerationException("The AI response wasn't valid JSON.", GenerationException.Code.INVALID_JSON);
        }
        JsonNode value = parsed == null ? null : parsed.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new GenerationException("The AI response had no " + field + ".",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED);
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new GenerationException("The AI " + field + " was too long.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED);
        }
        contentSafetyValidator.validate(field, text);
        return text;
    }
}
