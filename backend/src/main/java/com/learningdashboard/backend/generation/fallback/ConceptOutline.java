package com.learningdashboard.backend.generation.fallback;

import java.util.List;

/**
 * What {@link ConceptTextOutliner} can recover from raw concept text
 * without any AI: a title, a one-line summary, and the text's own points
 * in their original order. Every word in here is the user's own - nothing
 * is inferred, summarized or invented.
 *
 * @param sequential true when the text reads as ordered steps (a numbered
 *                   list, or "first ... then ... finally")
 */
public record ConceptOutline(String title, String summary, List<Point> points, boolean sequential) {

    public ConceptOutline {
        points = List.copyOf(points);
    }

    /**
     * @param label  a short form of the point, safe for a node/box/scene title
     * @param detail the full sentence or list item it came from
     * @param group  the heading this point sat under in the source text, or null
     */
    public record Point(String label, String detail, String group) { }
}
