package com.learningdashboard.backend.generation.model;

import java.util.List;

/**
 * A true graph, not just a tree: {@code edges} can connect any two nodes
 * (a concept can relate to more than one parent), which the original
 * {@code parentId}-only shape couldn't express.
 */
public record MindMapPayload(
        int version,
        String rootLabel,
        List<Node> nodes,
        List<Edge> edges,
        LayoutHints layoutHints,
        List<Citation> citations
) implements VisualizationPayload {

    @Override
    public String type() { return "mind_map"; }

    public record Node(String id, String label, String detail) { }

    public record Edge(String sourceId, String targetId, String relationshipLabel) { }

    /** Author intent for rendering, so the frontend isn't guessing a layout algorithm from scratch. */
    public record LayoutHints(String orientation, String rootNodeId) { }

    /** Where a node's claim came from - "which source" not "why to trust it"; author must ground claims in input. */
    public record Citation(String nodeId, String sourceText) { }
}
