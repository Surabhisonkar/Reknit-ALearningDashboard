package com.learningdashboard.backend.generation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import org.junit.jupiter.api.Test;

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
