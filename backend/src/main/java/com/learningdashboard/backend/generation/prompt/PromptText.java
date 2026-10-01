package com.learningdashboard.backend.generation.prompt;

/**
 * Makes user-supplied text safe to place inside the prompt's XML-style tags:
 * angle brackets become look-alike characters, so a message can't close a
 * tag (e.g. "</question>") and smuggle in instructions. The model still reads
 * the text naturally ("a < b" becomes "a ‹ b").
 */
public final class PromptText {

    private PromptText() { }

    public static String neutralizeTags(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('<', '\u2039').replace('>', '\u203A');
    }
}
