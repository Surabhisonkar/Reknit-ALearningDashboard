package com.learningdashboard.backend.generation.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The hard trust boundary between "whatever the LLM said" and "what the
 * rest of the system (persistence, frontend renderers) is allowed to
 * assume exists". Nothing downstream of this class ever sees unvalidated
 * model output. Mirrors the four {@link
 * com.learningdashboard.backend.generation.model.VisualizationPayload}
 * shapes field-for-field.
 */
@Component
public class VisualPayloadValidator {

    private static final Set<String> ELEMENT_TYPES =
            Set.of("process", "decision", "terminator", "data_store", "actor", "note");
    private static final Set<String> TYPES = Set.of("mind_map", "diagram", "animation", "image");

    public void validate(JsonNode payload) {
        List<String> errors = new ArrayList<>();
        String type = payload.path("type").asText(null);

        if (type == null || !TYPES.contains(type)) {
            throw new GenerationException(
                    "visualization.type must be one of mind_map, diagram, animation, image",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, List.of("type"));
        }

        switch (type) {
            case "mind_map" -> validateMindMap(payload, errors);
            case "diagram" -> validateDiagram(payload, errors);
            case "animation" -> validateAnimation(payload, errors);
            case "image" -> validateImage(payload, errors);
            default -> { /* unreachable, guarded above */ }
        }

        if (!errors.isEmpty()) {
            throw new GenerationException(
                    "The AI response didn't match the expected visualization structure.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, errors);
        }
    }

    private void validateMindMap(JsonNode p, List<String> errors) {
        requireNonBlank(p, "rootLabel", 80, errors);
        JsonNode nodes = p.path("nodes");
        Set<String> nodeIds = new HashSet<>();
        if (!nodes.isArray() || nodes.isEmpty() || nodes.size() > 15) {
            errors.add("nodes must contain between 1 and 15 items");
        } else {
            for (JsonNode node : nodes) {
                requireNonBlank(node, "id", 40, errors);
                requireNonBlank(node, "label", 80, errors);
                nodeIds.add(node.path("id").asText());
            }
        }
        for (JsonNode edge : p.path("edges")) {
            requireNonBlank(edge, "sourceId", 40, errors);
            requireNonBlank(edge, "targetId", 40, errors);
            if (!nodeIds.contains(edge.path("sourceId").asText()) || !nodeIds.contains(edge.path("targetId").asText())) {
                errors.add("edges must reference node ids that exist in nodes[]");
            }
        }
    }

    private void validateDiagram(JsonNode p, List<String> errors) {
        JsonNode elements = p.path("elements");
        Set<String> elementIds = new HashSet<>();
        if (!elements.isArray() || elements.isEmpty() || elements.size() > 20) {
            errors.add("elements must contain between 1 and 20 items");
        } else {
            for (JsonNode el : elements) {
                requireNonBlank(el, "id", 40, errors);
                requireNonBlank(el, "label", 80, errors);
                String elementType = el.path("elementType").asText(null);
                if (elementType == null || !ELEMENT_TYPES.contains(elementType)) {
                    errors.add("element.elementType must be one of " + ELEMENT_TYPES);
                }
                requireNonBlank(el, "accessibilityLabel", 160, errors);
                elementIds.add(el.path("id").asText());
            }
        }
        for (JsonNode conn : p.path("connections")) {
            requireNonBlank(conn, "sourceId", 40, errors);
            requireNonBlank(conn, "targetId", 40, errors);
            if (!elementIds.contains(conn.path("sourceId").asText()) || !elementIds.contains(conn.path("targetId").asText())) {
                errors.add("connections must reference element ids that exist in elements[]");
            }
        }
    }

    private void validateAnimation(JsonNode p, List<String> errors) {
        JsonNode scenes = p.path("scenes");
        if (!scenes.isArray() || scenes.isEmpty() || scenes.size() > 12) {
            errors.add("scenes must contain between 1 and 12 items");
            return;
        }
        for (JsonNode scene : scenes) {
            requireNonBlank(scene, "id", 40, errors);
            requireNonBlank(scene, "title", 80, errors);
            requireNonBlank(scene, "narration", 600, errors);
            if (!scene.path("order").isInt() || scene.path("order").asInt() < 0) {
                errors.add("scene.order must be a non-negative integer");
            }
            double duration = scene.path("durationSeconds").asDouble(-1);
            if (duration <= 0 || duration > 60) {
                errors.add("scene.durationSeconds must be between 0 and 60");
            }
            if (scene.path("needsVisualAsset").asBoolean(false)
                    && scene.path("visualPrompt").asText("").isBlank()) {
                errors.add("scene.visualPrompt is required when needsVisualAsset is true");
            }
        }
    }

    private void validateImage(JsonNode p, List<String> errors) {
        requireNonBlank(p, "imagePrompt", 500, errors);
        requireNonBlank(p, "altText", 160, errors);
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
