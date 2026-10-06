package com.learningdashboard.backend.generation.fallback;

import com.learningdashboard.backend.generation.model.MindMapPayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mind map: the title is the root. Text with headings becomes two levels
 * (root -> heading -> its points); flat text becomes one ring of points
 * around the root. Each node cites the sentence it came from.
 */
@Component
public class MindMapFallbackBuilder implements FallbackVisualBuilder {

    /** VisualPayloadValidator allows 15 nodes including the root. */
    static final int MAX_NODES = 15;
    static final String ROOT_ID = "root";

    @Override
    public String type() {
        return "mind_map";
    }

    @Override
    public VisualizationPayload build(ConceptOutline outline) {
        List<MindMapPayload.Node> nodes = new ArrayList<>();
        List<MindMapPayload.Edge> edges = new ArrayList<>();
        List<MindMapPayload.Citation> citations = new ArrayList<>();

        String rootLabel = ConceptTextOutliner.shorten(outline.title(), 80);
        nodes.add(new MindMapPayload.Node(ROOT_ID, rootLabel, outline.summary()));

        Map<String, String> groupNodeIds = groupNodeIds(outline);
        groupNodeIds.forEach((group, id) -> {
            nodes.add(new MindMapPayload.Node(id, group, ""));
            edges.add(new MindMapPayload.Edge(ROOT_ID, id, ""));
        });

        int pointNumber = 0;
        for (ConceptOutline.Point point : outline.points()) {
            if (nodes.size() >= MAX_NODES) {
                break;
            }
            String id = "n" + (++pointNumber);
            // A point whose label is the whole sentence needs no separate detail.
            String detail = point.detail().equals(point.label()) ? "" : point.detail();
            nodes.add(new MindMapPayload.Node(id, point.label(), detail));
            edges.add(new MindMapPayload.Edge(groupNodeIds.getOrDefault(point.group(), ROOT_ID), id, ""));
            citations.add(new MindMapPayload.Citation(id, point.detail()));
        }

        return new MindMapPayload(1, rootLabel, nodes, edges,
                new MindMapPayload.LayoutHints("radial", ROOT_ID), citations);
    }

    /**
     * Headings become branch nodes only when there are at least two of
     * them and they still leave room for points; otherwise the map is flat.
     */
    private Map<String, String> groupNodeIds(ConceptOutline outline) {
        Map<String, String> ids = new LinkedHashMap<>();
        for (ConceptOutline.Point point : outline.points()) {
            if (point.group() != null && !ids.containsKey(point.group())) {
                ids.put(point.group(), "g" + (ids.size() + 1));
            }
        }
        if (ids.size() < 2 || ids.size() > (MAX_NODES - 1) / 2) {
            ids.clear();
        }
        return ids;
    }
}
