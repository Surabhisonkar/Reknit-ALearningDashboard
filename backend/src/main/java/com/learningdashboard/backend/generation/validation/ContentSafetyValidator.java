package com.learningdashboard.backend.generation.validation;

import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.config.ContentSafetyProperties;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Runs on every user-visible text field an AI provider produces
 * (explanations, concept titles/summaries), after {@link
 * VisualPayloadValidator}'s structural checks but before anything is
 * persisted or returned to the client. Two independent things happen
 * here:
 *
 * <ol>
 *   <li>Structural checks that don't need any configured list at all:
 *       control characters (defensive against odd encoding tricks) and
 *       HTML/script-tag injection (defense in depth — this is a JSON
 *       API today with no HTML rendering of AI output, but a future
 *       export/share feature that does render this as HTML shouldn't
 *       have to remember to add this check retroactively).</li>
 *   <li>An operator-extensible disallowed-terms list ({@code
 *       CONTENT_SAFETY_DISALLOWED_TERMS}, empty by default) — this is
 *       intentionally not a content-moderation ML model; it's a cheap
 *       backstop, most useful once this product has any feature where
 *       one user's generated content becomes visible to others.</li>
 * </ol>
 */
@Component
public class ContentSafetyValidator {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    private static final Pattern HTML_INJECTION = Pattern.compile("(?i)<\\s*(script|iframe|object|embed)\\b");

    private final List<String> disallowedTermsLowercase;

    public ContentSafetyValidator(ContentSafetyProperties properties) {
        this.disallowedTermsLowercase = properties.getDisallowedTerms().stream()
                .map(String::toLowerCase)
                .filter(t -> !t.isBlank())
                .toList();
    }

    /** @throws GenerationException with code CONTENT_SAFETY_REJECTED if {@code text} fails any check. */
    public void validate(String fieldLabel, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (CONTROL_CHARS.matcher(text).find()) {
            throw reject(fieldLabel, "contains invalid control characters");
        }

        if (HTML_INJECTION.matcher(text).find()) {
            throw reject(fieldLabel, "contains disallowed markup");
        }

        String lower = text.toLowerCase();
        for (String term : disallowedTermsLowercase) {
            if (lower.contains(term)) {
                // Never echo which term matched back to the caller - that's
                // free information about the filter's contents.
                throw reject(fieldLabel, "contains disallowed content");
            }
        }
    }

    private GenerationException reject(String fieldLabel, String reason) {
        return new GenerationException(
                "Generated content was rejected by content-safety checks.",
                GenerationException.Code.CONTENT_SAFETY_REJECTED,
                List.of(fieldLabel + " " + reason));
    }
}
