package com.learningdashboard.backend.generation.model;

import java.util.List;

/**
 * Version 2 (what new generations produce) is Mermaid source: the model
 * writes {@code source} in {@code syntax} "mermaid", the frontend renders
 * it to SVG, so labels and arrows stay real text the user can read and
 * edit. Version 1 rows (still in the database) carry the older
 * positioned {@code elements}/{@code connections} instead.
 *
 * <p>For structured processes/architectures/flowcharts — distinct from
 * {@link MindMapPayload} (organic branching ideas) and from {@link
 * ImagePayload} (a single picture with no internal structure).
 */
public record DiagramPayload(
        int version,
        List<Element> elements,
        List<Connection> connections,
        String syntax,
        String source,
        String caption
) implements VisualizationPayload {

    @Override
    public String type() { return "diagram"; }

    /**
     * {@code elementType} is one of a small controlled vocabulary the
     * frontend renderer switches on: process | decision | terminator |
     * data_store | actor | note. Coordinates are author-suggested layout,
     * not pixel-perfect - the frontend renderer may still auto-arrange.
     */
    public record Element(
            String id, String elementType, String label,
            double x, double y, double width, double height,
            String style, String accessibilityLabel
    ) { }

    public record Connection(String id, String sourceId, String targetId, String label, String style) { }
}
