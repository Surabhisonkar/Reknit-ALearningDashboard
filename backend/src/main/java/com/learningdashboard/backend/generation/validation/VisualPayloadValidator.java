package com.learningdashboard.backend.generation.validation;

import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

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
    private static final Set<String> MERMAID_HEADERS = Set.of(
            "flowchart", "graph", "sequenceDiagram", "stateDiagram", "stateDiagram-v2",
            "classDiagram", "erDiagram", "timeline");
    // Mermaid features that can run script or pull in outside content, even under securityLevel "strict".
    private static final Pattern MERMAID_DISALLOWED =
            Pattern.compile("(?i)(\\bclick\\b|\\bhref\\b|javascript:|%%\\{|<\\s*[a-z/!]|\\bcall\\s)");
    private static final Set<String> SCENE_TRANSITIONS = Set.of("fade", "slide", "wipe", "zoom", "cut", "none");
    private static final Set<String> ELEMENT_KINDS = Set.of("text", "box", "circle", "arrow", "emoji");
    private static final Set<String> ELEMENT_ANIMATIONS = Set.of("fade", "pop", "slide", "draw", "pulse", "highlight");
    private static final Set<String> SCENE_LAYOUTS = Set.of("free", "steps", "cycle", "compare", "before_after", "focus");
    // An icon is an emoji (possibly a multi-code-point sequence), never words.
    private static final Pattern ICON_TEXT = Pattern.compile("[\\p{L}\\p{N}]");

    public void validate(JsonNode payload) {
        List<String> errors = new ArrayList<>();
        String type = payload.path("type").asText(null);

        if (type == null || !TYPES.contains(type)) {
            throw new GenerationException(
                    "visualization.type must be one of mind_map, diagram, animation, image",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, List.of("type"));
        }

        // Version 2 is what the current prompt asks for; version 1 checks stay for the older shapes.
        boolean v2 = payload.path("version").asInt(1) >= 2;
        switch (type) {
            case "mind_map" -> validateMindMap(payload, v2, errors);
            case "diagram" -> {
                if (v2) validateMermaidDiagram(payload, errors);
                else validateDiagram(payload, errors);
            }
            case "animation" -> validateAnimation(payload, v2, errors);
            case "image" -> validateImage(payload, v2, errors);
            default -> { /* unreachable, guarded above */ }
        }

        if (!errors.isEmpty()) {
            throw new GenerationException(
                    "The AI response didn't match the expected visualization structure.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, errors);
        }
    }

    private void validateMindMap(JsonNode p, boolean v2, List<String> errors) {
        requireNonBlank(p, "rootLabel", 80, errors);
        JsonNode nodes = p.path("nodes");
        Set<String> nodeIds = new HashSet<>();
        int maxNodes = v2 ? 25 : 15;
        if (!nodes.isArray() || nodes.isEmpty() || nodes.size() > maxNodes) {
            errors.add("nodes must contain between 1 and " + maxNodes + " items");
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
        if (v2 && errors.isEmpty()) {
            validateMindMapTree(p, errors);
        }
    }

    /** Exactly one root (the layoutHints.rootNodeId), and every other node's parentId chain reaches it. */
    private void validateMindMapTree(JsonNode p, List<String> errors) {
        String rootId = p.path("layoutHints").path("rootNodeId").asText("");
        Map<String, String> parentOf = new HashMap<>();
        for (JsonNode node : p.path("nodes")) {
            JsonNode parent = node.path("parentId");
            parentOf.put(node.path("id").asText(), parent.isTextual() && !parent.asText().isBlank() ? parent.asText() : null);
        }
        if (!parentOf.containsKey(rootId) || parentOf.get(rootId) != null) {
            errors.add("layoutHints.rootNodeId must name the node whose parentId is null");
            return;
        }
        for (var entry : parentOf.entrySet()) {
            String current = entry.getKey();
            int hops = 0;
            while (current != null && !current.equals(rootId)) {
                current = parentOf.get(current);
                if (current == null || !parentOf.containsKey(current) || ++hops > parentOf.size()) {
                    errors.add("node " + entry.getKey() + " must descend from the root through parentId");
                    return;
                }
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

    private void validateMermaidDiagram(JsonNode p, List<String> errors) {
        if (!"mermaid".equals(p.path("syntax").asText())) {
            errors.add("syntax must be mermaid");
        }
        requireNonBlank(p, "source", 4000, errors);
        String source = p.path("source").asText("").strip();
        String header = source.split("\\s+", 2)[0];
        if (!source.isEmpty() && !MERMAID_HEADERS.contains(header)) {
            errors.add("source must start with one of " + MERMAID_HEADERS);
        }
        if (MERMAID_DISALLOWED.matcher(source).find()) {
            errors.add("source must not contain click/href directives, init blocks or HTML");
        }
    }

    private void validateAnimation(JsonNode p, boolean v2, List<String> errors) {
        JsonNode scenes = p.path("scenes");
        int minScenes = v2 ? 4 : 1;
        int maxScenes = v2 ? 8 : 12;
        if (!scenes.isArray() || scenes.size() < minScenes || scenes.size() > maxScenes) {
            errors.add("scenes must contain between " + minScenes + " and " + maxScenes + " items");
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
            if (v2) {
                validateScene(scene, duration, errors);
            }
        }
    }

    private void validateScene(JsonNode scene, double duration, List<String> errors) {
        String transition = scene.path("transitionToNext").asText("none");
        if (!SCENE_TRANSITIONS.contains(transition)) {
            errors.add("scene.transitionToNext must be one of " + SCENE_TRANSITIONS);
        }
        String layout = scene.path("layout").asText("free");
        if (!SCENE_LAYOUTS.contains(layout)) {
            errors.add("scene.layout must be one of " + SCENE_LAYOUTS);
        }
        // A layout places every element itself, so only free scenes need positions.
        boolean placedByLayout = !"free".equals(layout);
        JsonNode elements = scene.path("visualElements");
        if (!elements.isArray() || elements.isEmpty() || elements.size() > 6) {
            errors.add("scene.visualElements must contain between 1 and 6 items");
            return;
        }
        Set<String> ids = new HashSet<>();
        elements.forEach(el -> ids.add(el.path("id").asText()));
        for (JsonNode el : elements) {
            requireNonBlank(el, "id", 40, errors);
            String kind = el.path("kind").asText("");
            if (!ELEMENT_KINDS.contains(kind)) {
                errors.add("visualElement.kind must be one of " + ELEMENT_KINDS);
                continue;
            }
            if ("arrow".equals(kind)) {
                if (!ids.contains(el.path("fromId").asText()) || !ids.contains(el.path("toId").asText())) {
                    errors.add("arrow visualElements must connect two element ids from the same scene");
                }
                String label = el.path("text").asText("").trim();
                if (label.length() > 24 || (!label.isEmpty() && label.split("\\s+").length > 3)) {
                    errors.add("an arrow's text label must be at most 3 words");
                }
            } else {
                requireNonBlank(el, "text", 60, errors);
                if (!placedByLayout) {
                    requireRange(el, "x", 0, 100, errors);
                    requireRange(el, "y", 0, 100, errors);
                }
            }
            String icon = el.path("icon").asText("");
            if (!icon.isEmpty() && (icon.length() > 16 || ICON_TEXT.matcher(icon).find())) {
                errors.add("visualElement.icon must be a single emoji");
            }
            String animation = el.path("animation").asText("fade");
            if (!ELEMENT_ANIMATIONS.contains(animation)) {
                errors.add("visualElement.animation must be one of " + ELEMENT_ANIMATIONS);
            }
            double enterAt = el.path("enterAtSeconds").asDouble(0);
            if (enterAt < 0 || enterAt >= duration) {
                errors.add("visualElement.enterAtSeconds must be within the scene's duration");
            }
        }
    }

    private void validateImage(JsonNode p, boolean v2, List<String> errors) {
        requireNonBlank(p, "imagePrompt", v2 ? 2000 : 500, errors);
        requireNonBlank(p, "altText", 160, errors);
        if (!v2) {
            return;
        }
        JsonNode labels = p.path("labels");
        if (!labels.isArray() || labels.size() < 2 || labels.size() > 8) {
            errors.add("labels must contain between 2 and 8 items");
            return;
        }
        for (JsonNode label : labels) {
            requireNonBlank(label, "id", 40, errors);
            requireNonBlank(label, "text", 60, errors);
            requireNonBlank(label, "explanation", 240, errors);
            requireRange(label, "x", 0, 1, errors);
            requireRange(label, "y", 0, 1, errors);
        }
    }

    private void requireRange(JsonNode parent, String field, double min, double max, List<String> errors) {
        JsonNode value = parent.path(field);
        if (!value.isNumber() || value.asDouble() < min || value.asDouble() > max) {
            errors.add(field + " must be a number between " + (int) min + " and " + (int) max);
        }
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
