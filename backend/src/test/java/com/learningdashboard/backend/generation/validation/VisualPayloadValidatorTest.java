package com.learningdashboard.backend.generation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learningdashboard.backend.common.exception.GenerationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class VisualPayloadValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final VisualPayloadValidator validator = new VisualPayloadValidator();

    @Test
    void acceptsValidMindMap() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "mind_map", "version": 1, "rootLabel": "Cell",
                  "nodes": [{ "id": "n1", "label": "Nucleus", "detail": "Stores DNA" }],
                  "edges": [],
                  "layoutHints": { "orientation": "radial", "rootNodeId": "n1" },
                  "citations": []
                }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMindMapEdgeReferencingUnknownNode() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "mind_map", "version": 1, "rootLabel": "Cell",
                  "nodes": [{ "id": "n1", "label": "Nucleus" }],
                  "edges": [{ "sourceId": "n1", "targetId": "does-not-exist" }]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsValidDiagram() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "diagram", "version": 1,
                  "elements": [{ "id": "e1", "elementType": "process", "label": "Start",
                                  "x": 0, "y": 0, "width": 100, "height": 50,
                                  "style": "", "accessibilityLabel": "Process start node" }],
                  "connections": []
                }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDiagramWithInvalidElementType() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "diagram", "version": 1,
                  "elements": [{ "id": "e1", "elementType": "not_a_real_type", "label": "Start",
                                  "accessibilityLabel": "x" }],
                  "connections": []
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsValidAnimationWithModelChosenSceneCount() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "animation", "version": 1,
                  "scenes": [
                    { "id": "s1", "order": 0, "title": "Intro", "narration": "It begins.",
                      "durationSeconds": 4, "transitionToNext": "fade",
                      "needsVisualAsset": false, "visualPrompt": "", "assetArtifactIds": [] },
                    { "id": "s2", "order": 1, "title": "Middle", "narration": "It continues.",
                      "durationSeconds": 6, "transitionToNext": "cut",
                      "needsVisualAsset": true, "visualPrompt": "a concrete image prompt", "assetArtifactIds": [] }
                  ]
                }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnimationSceneNeedingAssetWithoutPrompt() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "animation", "version": 1,
                  "scenes": [{ "id": "s1", "order": 0, "title": "Intro", "narration": "It begins.",
                                "durationSeconds": 4, "transitionToNext": "fade",
                                "needsVisualAsset": true, "visualPrompt": "", "assetArtifactIds": [] }]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsAnimationSceneWithExcessiveDuration() throws Exception {
        JsonNode payload = mapper.readTree("""
                {
                  "type": "animation", "version": 1,
                  "scenes": [{ "id": "s1", "order": 0, "title": "Intro", "narration": "It begins.",
                                "durationSeconds": 999, "transitionToNext": "fade",
                                "needsVisualAsset": false, "visualPrompt": "", "assetArtifactIds": [] }]
                }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsValidImage() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "image", "version": 1, "imagePrompt": "A golden retriever in a field",
                  "artifactId": "", "altText": "A golden retriever standing in a sunny field" }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void acceptsMermaidDiagram() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "diagram", "version": 2, "syntax": "mermaid", "caption": "Handshake",
                  "source": "flowchart LR\\n  A[\\"Client\\"] -->|\\"SYN\\"| B[\\"Server\\"]" }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMermaidWithClickDirective() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "diagram", "version": 2, "syntax": "mermaid",
                  "source": "flowchart LR\\n  A --> B\\n  click A \\"https://example.com\\"" }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsMermaidWithUnknownHeader() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "diagram", "version": 2, "syntax": "mermaid", "source": "pie title X" }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsMindMapTree() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "mind_map", "version": 2, "rootLabel": "Cell",
                  "nodes": [{ "id": "r", "label": "Cell", "parentId": null },
                            { "id": "a", "label": "Nucleus", "parentId": "r" },
                            { "id": "b", "label": "DNA", "parentId": "a" }],
                  "edges": [], "layoutHints": { "orientation": "horizontal", "rootNodeId": "r" } }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMindMapCycle() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "mind_map", "version": 2, "rootLabel": "Cell",
                  "nodes": [{ "id": "r", "label": "Cell", "parentId": null },
                            { "id": "a", "label": "A", "parentId": "b" },
                            { "id": "b", "label": "B", "parentId": "a" }],
                  "edges": [], "layoutHints": { "orientation": "horizontal", "rootNodeId": "r" } }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsLabelledIllustration() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "image", "version": 2, "imagePrompt": "A plant cell cross-section, no text",
                  "artifactId": "", "altText": "A plant cell",
                  "labels": [{ "id": "l1", "text": "Nucleus", "explanation": "Holds DNA", "x": 0.5, "y": 0.4 },
                             { "id": "l2", "text": "Wall", "explanation": "Rigid outer layer", "x": 0.1, "y": 0.9 }] }
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsIllustrationLabelOutsideImage() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "image", "version": 2, "imagePrompt": "x", "artifactId": "", "altText": "x",
                  "labels": [{ "id": "l1", "text": "A", "explanation": "a", "x": 1.5, "y": 0.4 },
                             { "id": "l2", "text": "B", "explanation": "b", "x": 0.1, "y": 0.9 }] }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void acceptsStoryboardAndRejectsTooFewScenes() throws Exception {
        String scene = """
                { "id": "s%d", "order": %d, "title": "T", "narration": "N", "durationSeconds": 5,
                  "transitionToNext": "fade", "needsVisualAsset": false, "visualPrompt": "", "assetArtifactIds": [],
                  "visualElements": [
                    { "id": "a", "kind": "box", "text": "Client", "x": 25, "y": 50, "enterAtSeconds": 0, "animation": "pop", "color": "teal" },
                    { "id": "b", "kind": "box", "text": "Server", "x": 75, "y": 50, "enterAtSeconds": 1, "animation": "pop", "color": "coral" },
                    { "id": "c", "kind": "arrow", "fromId": "a", "toId": "b", "enterAtSeconds": 2, "animation": "draw" } ] }
                """;
        String four = String.join(",", scene.formatted(1, 1), scene.formatted(2, 2), scene.formatted(3, 3), scene.formatted(4, 4));
        JsonNode ok = mapper.readTree("{ \"type\": \"animation\", \"version\": 2, \"scenes\": [" + four + "] }");
        assertThatCode(() -> validator.validate(ok)).doesNotThrowAnyException();

        JsonNode tooFew = mapper.readTree("{ \"type\": \"animation\", \"version\": 2, \"scenes\": [" + scene.formatted(1, 1) + "] }");
        assertThatThrownBy(() -> validator.validate(tooFew)).isInstanceOf(GenerationException.class);
    }

    /** Four scenes built from one template; %s is replaced by the scene's layout and elements. */
    private JsonNode storyboard(String layoutAndElements) throws Exception {
        String scene = """
                { "id": "s%d", "order": %d, "title": "T", "narration": "N", "durationSeconds": 5,
                  "transitionToNext": "fade", "needsVisualAsset": false, "visualPrompt": "", "assetArtifactIds": [],
                  LAYOUT_AND_ELEMENTS }
                """.replace("LAYOUT_AND_ELEMENTS", layoutAndElements);
        String four = String.join(",", scene.formatted(1, 1), scene.formatted(2, 2), scene.formatted(3, 3), scene.formatted(4, 4));
        return mapper.readTree("{ \"type\": \"animation\", \"version\": 2, \"scenes\": [" + four + "] }");
    }

    @Test
    void acceptsALayoutSceneWithoutPositionsIconsArrowLabelsAndHighlight() throws Exception {
        JsonNode payload = storyboard("""
                "layout": "steps", "visualElements": [
                  { "id": "a", "kind": "box", "text": "Warm sea", "icon": "🌊", "enterAtSeconds": 0, "animation": "pop", "color": "teal" },
                  { "id": "b", "kind": "box", "text": "Vapour", "icon": "♨️", "enterAtSeconds": 1, "animation": "highlight", "color": "yellow" },
                  { "id": "c", "kind": "arrow", "text": "heat", "fromId": "a", "toId": "b", "enterAtSeconds": 2, "animation": "draw" } ]
                """);
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void stillRequiresPositionsInAFreeScene() throws Exception {
        JsonNode payload = storyboard("""
                "layout": "free", "visualElements": [
                  { "id": "a", "kind": "box", "text": "Client", "enterAtSeconds": 0, "animation": "pop", "color": "teal" } ]
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsAnUnknownLayoutAWordyIconAndALongArrowLabel() throws Exception {
        String elements = """
                  { "id": "a", "kind": "box", "text": "A", "enterAtSeconds": 0, "animation": "pop", "color": "teal" },
                  { "id": "b", "kind": "box", "text": "B", "enterAtSeconds": 0, "animation": "pop", "color": "teal" },
                  { "id": "c", "kind": "arrow", "text": "LABEL", "fromId": "a", "toId": "b", "enterAtSeconds": 0, "animation": "draw" } ]
                """;
        JsonNode unknownLayout = storyboard("\"layout\": \"spiral\", \"visualElements\": [" + elements.replace("LABEL", "heat"));
        JsonNode wordyIcon = storyboard("\"layout\": \"steps\", \"visualElements\": ["
                + elements.replace("LABEL", "heat").replaceFirst("\"text\": \"A\"", "\"text\": \"A\", \"icon\": \"sun\""));
        JsonNode longLabel = storyboard("\"layout\": \"steps\", \"visualElements\": ["
                + elements.replace("LABEL", "this label is far too long"));
        assertThatThrownBy(() -> validator.validate(unknownLayout)).isInstanceOf(GenerationException.class);
        assertThatThrownBy(() -> validator.validate(wordyIcon)).isInstanceOf(GenerationException.class);
        assertThatThrownBy(() -> validator.validate(longLabel)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsUnknownVisualizationType() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "type": "video", "url": "https://example.com" }
                """);
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }

    @Test
    void rejectsMissingType() throws Exception {
        JsonNode payload = mapper.readTree("{ \"rootLabel\": \"X\" }");
        assertThatThrownBy(() -> validator.validate(payload)).isInstanceOf(GenerationException.class);
    }
}
