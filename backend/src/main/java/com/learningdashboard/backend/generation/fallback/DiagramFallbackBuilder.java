package com.learningdashboard.backend.generation.fallback;

import com.learningdashboard.backend.generation.model.DiagramPayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Flowchart: the title is the start node and the text's points follow it
 * top to bottom in their original order. A point phrased as a condition
 * ("if ...", "whether ...", or a question) becomes a decision; a heading
 * in the source text becomes a note that opens its own run of steps.
 */
@Component
public class DiagramFallbackBuilder implements FallbackVisualBuilder {

    /** VisualPayloadValidator allows 20 elements. */
    static final int MAX_ELEMENTS = 20;

    private static final Pattern CONDITION = Pattern.compile("(?i)^(if|whether|when|unless)\\b.*|.*\\?$");
    private static final double COLUMN_X = 310;
    private static final double WIDTH = 180;
    private static final double HEIGHT = 60;
    private static final double ROW_GAP = 110;

    @Override
    public String type() {
        return "diagram";
    }

    @Override
    public VisualizationPayload build(ConceptOutline outline) {
        List<DiagramPayload.Element> elements = new ArrayList<>();
        List<DiagramPayload.Connection> connections = new ArrayList<>();

        String title = ConceptTextOutliner.shorten(outline.title(), 80);
        elements.add(element("start", "terminator", title, title, 0));

        String previousId = "start";
        String currentGroup = null;
        int stepNumber = 0;
        int groupNumber = 0;

        for (ConceptOutline.Point point : outline.points()) {
            boolean opensGroup = point.group() != null && !point.group().equals(currentGroup);
            if (elements.size() + (opensGroup ? 2 : 1) > MAX_ELEMENTS) {
                break;
            }
            if (opensGroup) {
                currentGroup = point.group();
                String groupId = "g" + (++groupNumber);
                elements.add(element(groupId, "note", currentGroup, "Section: " + currentGroup, elements.size()));
                connections.add(connect(previousId, groupId));
                previousId = groupId;
            }
            String id = "e" + (++stepNumber);
            String elementType = CONDITION.matcher(point.detail()).matches() ? "decision" : "process";
            elements.add(element(id, elementType, point.label(), point.detail(), elements.size()));
            connections.add(connect(previousId, id));
            previousId = id;
        }

        return new DiagramPayload(1, elements, connections);
    }

    private DiagramPayload.Element element(String id, String elementType, String label, String description, int row) {
        return new DiagramPayload.Element(id, elementType, ConceptTextOutliner.shorten(label, 80),
                COLUMN_X, 20 + row * ROW_GAP, WIDTH, HEIGHT, "",
                ConceptTextOutliner.shorten(description, 160));
    }

    private DiagramPayload.Connection connect(String sourceId, String targetId) {
        return new DiagramPayload.Connection(sourceId + "-" + targetId, sourceId, targetId, "", "");
    }
}
