package com.learningdashboard.backend.generation.fallback;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Rule-based reading of concept text - the "no AI could be reached" half
 * of the visual failsafe. Deterministic and free of I/O: the same text
 * always produces the same {@link ConceptOutline}.
 *
 * <p>It understands the shapes Capture actually sends: free prose, the
 * user's bullet/numbered notes, and the flattened AI explanation
 * ("Title", blank line, overview, then "Heading:" blocks of "- bullets").
 * It only reorganizes the user's words; it never summarizes or infers.
 */
@Component
public class ConceptTextOutliner {

    /** Enough for every builder's own cap; each builder trims further to its schema limit. */
    static final int MAX_POINTS = 18;
    static final int MAX_TITLE = 80;
    static final int MAX_LABEL = 60;
    static final int MAX_LABEL_WORDS = 7;
    static final int MAX_DETAIL = 300;
    static final int MAX_SUMMARY = 200;
    static final String DEFAULT_TITLE = "Your notes";

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    private static final Pattern NUMBERED_ITEM = Pattern.compile("^\\(?\\d{1,2}[.):]\\s+(.*)$");
    private static final Pattern BULLET_ITEM = Pattern.compile("^[-*\\u2022\\u2013\\u2014]\\s+(.*)$");
    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?])\\s+(?=[\\p{Lu}\\d\"'(])");
    private static final Pattern STEP_WORDS = Pattern.compile(
            "(?i)\\b(first|firstly|second|secondly|third|then|next|after that|afterwards|finally|lastly|step \\d+)\\b");
    private static final Pattern KEY_VALUE = Pattern.compile("^([^:]{2,40}):\\s+(\\S.*)$");
    private static final Pattern CLAUSE_BREAK = Pattern.compile("\\s+[-\\u2013\\u2014]\\s+|[,;:(]");
    private static final Set<String> TRAILING_FILLER = Set.of(
            "a", "an", "the", "of", "in", "on", "to", "and", "or", "for", "with", "by", "at", "from", "that",
            "which", "is", "are", "as", "into", "its", "their", "this");

    public ConceptOutline outline(String conceptText) {
        List<String> lines = cleanLines(conceptText);
        if (lines.isEmpty()) {
            return new ConceptOutline(DEFAULT_TITLE, DEFAULT_TITLE,
                    List.of(new ConceptOutline.Point(DEFAULT_TITLE, DEFAULT_TITLE, null)), false);
        }

        String first = lines.get(0);
        boolean firstLineIsTitle = lines.size() > 1 && first.length() <= MAX_TITLE
                && !isListItem(first) && !endsLikeSentence(first);
        String title = firstLineIsTitle ? stripTrailingColon(first) : shortLabel(firstSentence(first), MAX_TITLE, 12);
        List<String> body = firstLineIsTitle ? lines.subList(1, lines.size()) : lines;

        List<ConceptOutline.Point> points = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String group = null;
        int numberedItems = 0;
        boolean firstPointIsProse = false;

        for (String line : body) {
            if (isHeading(line)) {
                group = shorten(stripTrailingColon(line), MAX_LABEL);
                continue;
            }
            Matcher numbered = NUMBERED_ITEM.matcher(line);
            Matcher bullet = BULLET_ITEM.matcher(line);
            if (numbered.matches()) {
                numberedItems++;
                addPoint(points, seen, numbered.group(1), group);
            } else if (bullet.matches()) {
                addPoint(points, seen, bullet.group(1), group);
            } else {
                boolean wasEmpty = points.isEmpty();
                for (String sentence : SENTENCE_BREAK.split(line)) {
                    addPoint(points, seen, sentence, group);
                }
                if (wasEmpty && !points.isEmpty()) {
                    firstPointIsProse = true;
                }
            }
        }

        if (points.isEmpty()) {
            points.add(new ConceptOutline.Point(shorten(title, MAX_LABEL), shorten(title, MAX_DETAIL), null));
        }

        // A leading prose sentence is the natural one-line summary. Lift it out
        // of the points only when plenty remain, so short notes keep every point.
        String summary = shorten(points.get(0).detail(), MAX_SUMMARY);
        if (firstPointIsProse && points.size() >= 4) {
            points.remove(0);
        }

        boolean sequential = numberedItems >= 2 || countStepWords(body) >= 2;
        if (points.size() > MAX_POINTS) {
            points = new ArrayList<>(points.subList(0, MAX_POINTS));
        }
        return new ConceptOutline(title.isBlank() ? DEFAULT_TITLE : title, summary, points, sequential);
    }

    // ---- line handling ----------------------------------------------------

    private List<String> cleanLines(String conceptText) {
        List<String> lines = new ArrayList<>();
        if (conceptText == null) {
            return lines;
        }
        String cleaned = CONTROL_CHARS.matcher(conceptText).replaceAll(" ").replace('<', ' ').replace('>', ' ');
        for (String raw : cleaned.split("\\r?\\n")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            // Markdown emphasis/heading markers carry no meaning in a node label.
            line = line.replaceAll("^#{1,6}\\s+", "").replace("**", "").replace("__", "").trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private boolean isListItem(String line) {
        return NUMBERED_ITEM.matcher(line).matches() || BULLET_ITEM.matcher(line).matches();
    }

    private boolean isHeading(String line) {
        return line.endsWith(":") && line.length() <= MAX_LABEL + 1 && !isListItem(line);
    }

    private boolean endsLikeSentence(String line) {
        return line.endsWith(".") || line.endsWith("!") || line.endsWith("?");
    }

    private String stripTrailingColon(String line) {
        return line.endsWith(":") ? line.substring(0, line.length() - 1).trim() : line;
    }

    private String firstSentence(String line) {
        String[] sentences = SENTENCE_BREAK.split(line);
        return sentences.length == 0 ? line : sentences[0];
    }

    private int countStepWords(List<String> body) {
        int hits = 0;
        for (String line : body) {
            Matcher matcher = STEP_WORDS.matcher(line);
            while (matcher.find()) {
                hits++;
            }
        }
        return hits;
    }

    // ---- points -----------------------------------------------------------

    private void addPoint(List<ConceptOutline.Point> points, Set<String> seen, String rawText, String group) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.length() < 3 || !seen.add(text.toLowerCase(Locale.ROOT))) {
            return;
        }
        // "Chlorophyll: absorbs light" - the note-taker already wrote the label.
        Matcher keyValue = KEY_VALUE.matcher(text);
        if (keyValue.matches() && keyValue.group(1).split(" ").length <= 5) {
            points.add(new ConceptOutline.Point(
                    shorten(keyValue.group(1).trim(), MAX_LABEL), shorten(text, MAX_DETAIL), group));
            return;
        }
        points.add(new ConceptOutline.Point(shortLabel(text, MAX_LABEL, MAX_LABEL_WORDS), shorten(text, MAX_DETAIL), group));
    }

    /** The leading clause of {@code text}, cut to a word and character budget, ending cleanly. */
    static String shortLabel(String text, int maxChars, int maxWords) {
        String candidate = stripEndPunctuation(text.trim());
        Matcher clause = CLAUSE_BREAK.matcher(candidate);
        if (clause.find() && clause.start() > 0 && candidate.substring(0, clause.start()).trim().split(" ").length >= 2) {
            candidate = candidate.substring(0, clause.start()).trim();
        }

        String[] words = candidate.split(" ");
        int count = Math.min(words.length, maxWords);
        boolean truncated = words.length > maxWords;
        while (count > 1 && String.join(" ", List.of(words).subList(0, count)).length() > maxChars - 1) {
            count--;
            truncated = true;
        }
        if (truncated) {
            while (count > 2 && TRAILING_FILLER.contains(words[count - 1].toLowerCase(Locale.ROOT))) {
                count--;
            }
        }
        String label = stripEndPunctuation(String.join(" ", List.of(words).subList(0, count)));
        if (label.length() > maxChars - 1) {
            label = label.substring(0, maxChars - 1).trim();
            truncated = true;
        }
        return truncated ? label + "…" : label;
    }

    static String shorten(String text, int maxChars) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        String cut = trimmed.substring(0, maxChars - 1);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > maxChars / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut.trim() + "…";
    }

    private static String stripEndPunctuation(String text) {
        return text.replaceAll("[\\s.,;:!?\\u2013\\u2014-]+$", "");
    }
}
