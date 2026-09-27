package com.learningdashboard.backend.generation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.config.ContentSafetyProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentSafetyValidatorTest {

    @Test
    void acceptsOrdinaryText() {
        ContentSafetyValidator validator = validatorWithTerms(List.of());
        assertThatCode(() -> validator.validate("explanation", "Photosynthesis converts light into chemical energy."))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsNullAndEmptyTextWithoutThrowing() {
        ContentSafetyValidator validator = validatorWithTerms(List.of());
        assertThatCode(() -> validator.validate("field", null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("field", "")).doesNotThrowAnyException();
    }

    @Test
    void rejectsControlCharacters() {
        ContentSafetyValidator validator = validatorWithTerms(List.of());
        String withNullByte = "some text\u0000with a null byte";
        assertThatThrownBy(() -> validator.validate("explanation", withNullByte))
                .isInstanceOf(GenerationException.class)
                .extracting(e -> ((GenerationException) e).getCode())
                .isEqualTo(GenerationException.Code.CONTENT_SAFETY_REJECTED);
    }

    @Test
    void rejectsScriptTagInjection() {
        ContentSafetyValidator validator = validatorWithTerms(List.of());
        assertThatThrownBy(() -> validator.validate("summary", "Normal text <script>alert(1)</script> more text"))
                .isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsConfiguredDisallowedTermsCaseInsensitively() {
        ContentSafetyValidator validator = validatorWithTerms(List.of("bannedword"));
        assertThatThrownBy(() -> validator.validate("title", "This contains a BannedWord in it"))
                .isInstanceOf(GenerationException.class);
    }

    @Test
    void doesNotRejectTextWithoutAnyDisallowedTerm() {
        ContentSafetyValidator validator = validatorWithTerms(List.of("bannedword"));
        assertThatCode(() -> validator.validate("title", "This is a perfectly fine title"))
                .doesNotThrowAnyException();
    }

    private ContentSafetyValidator validatorWithTerms(List<String> terms) {
        ContentSafetyProperties props = new ContentSafetyProperties();
        props.setDisallowedTerms(terms);
        return new ContentSafetyValidator(props);
    }
}
