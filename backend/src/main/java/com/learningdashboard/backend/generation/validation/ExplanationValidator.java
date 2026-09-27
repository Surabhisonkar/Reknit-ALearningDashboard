package com.learningdashboard.backend.generation.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The trust boundary for {@code ExplanationPayload} - nothing downstream
 * (persistence, the frontend) ever sees an explanation that hasn't
 * passed through here. Enforces both structural shape (required fields,
 * length limits) and the product requirement that grounds this feature:
 * at least one analogy section and one example section, not just prose.
 */
@Component
public class ExplanationValidator {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_OVERVIEW_LENGTH = 400;
    private static final int MAX_HEADING_LENGTH = 80;
    private static final int MAX_BODY_LENGTH = 600;
    private static final int MAX_BULLET_LENGTH = 200;
    private static final int MIN_SECTIONS = 2;
    private static final int MAX_SECTIONS = 8;
    private static final Set<String> SECTION_TYPES = Set.of("concept", "analogy", "example");

    public void validate(JsonNode payload) {
        List<String> errors = new ArrayList<>();

        requireNonBlank(payload, "suggestedTitle", MAX_TITLE_LENGTH, errors);
        requireNonBlank(payload, "overview", MAX_OVERVIEW_LENGTH, errors);

        JsonNode sections = payload.path("sections");
        if (!sections.isArray() || sections.size() < MIN_SECTIONS || sections.size() > MAX_SECTIONS) {
            errors.add("sections must contain between " + MIN_SECTIONS + " and " + MAX_SECTIONS + " items");
        } else {
            boolean hasAnalogy = false;
            boolean hasExample = false;
            for (JsonNode section : sections) {
                String type = validateSection(section, errors);
                hasAnalogy |= "analogy".equals(type);
                hasExample |= "example".equals(type);
            }
            if (!hasAnalogy) {
                errors.add("sections must include at least one section of type \"analogy\"");
            }
            if (!hasExample) {
                errors.add("sections must include at least one section of type \"example\"");
            }
        }

        if (!errors.isEmpty()) {
            throw new GenerationException(
                    "The AI response didn't match the expected explanation structure.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, errors);
        }
    }

    /** @return the section's type if valid, so the caller can track analogy/example coverage; null otherwise. */
    private String validateSection(JsonNode section, List<String> errors) {
        requireNonBlank(section, "heading", MAX_HEADING_LENGTH, errors);

        String type = section.path("type").asText(null);
        if (type == null || !SECTION_TYPES.contains(type)) {
            errors.add("section.type must be one of " + SECTION_TYPES);
            return null;
        }

        String body = section.path("body").asText("");
        JsonNode bullets = section.path("bullets");
        boolean hasBody = !body.isBlank();
        boolean hasBullets = bullets.isArray() && !bullets.isEmpty();

        if (!hasBody && !hasBullets) {
            errors.add("section \"" + section.path("heading").asText() + "\" needs either body or bullets");
        }
        if (hasBody && body.length() > MAX_BODY_LENGTH) {
            errors.add("section body must be at most " + MAX_BODY_LENGTH + " characters");
        }
        if (hasBullets) {
            for (JsonNode bullet : bullets) {
                if (!bullet.isTextual() || bullet.asText().isBlank()) {
                    errors.add("bullets must be non-blank strings");
                } else if (bullet.asText().length() > MAX_BULLET_LENGTH) {
                    errors.add("each bullet must be at most " + MAX_BULLET_LENGTH + " characters");
                }
            }
        }

        return type;
    }

    private void requireNonBlank(JsonNode parent, String field, int maxLength, List<String> errors) {
        JsonNode value = parent.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            errors.add(field + " is required and must be a non-blank string");
        } else if (value.asText().length() > maxLength) {
            errors.add(field + " must be at most " + maxLength + " characters");
        }
    }
}
