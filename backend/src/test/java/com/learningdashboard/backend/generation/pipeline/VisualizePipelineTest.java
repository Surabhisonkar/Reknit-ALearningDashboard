package com.learningdashboard.backend.generation.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.config.ContentSafetyProperties;
import com.learningdashboard.backend.generation.fallback.AnimationFallbackBuilder;
import com.learningdashboard.backend.generation.fallback.ConceptTextOutliner;
import com.learningdashboard.backend.generation.fallback.DiagramFallbackBuilder;
import com.learningdashboard.backend.generation.fallback.FallbackVisualizationService;
import com.learningdashboard.backend.generation.fallback.MindMapFallbackBuilder;
import com.learningdashboard.backend.generation.model.ImagePayload;
import com.learningdashboard.backend.generation.provider.FallbackTextGenerationProvider;
import com.learningdashboard.backend.generation.provider.ProviderFallbackExecutor;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import com.learningdashboard.backend.generation.provider.VisualGenerationProvider;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import com.learningdashboard.backend.generation.validation.VisualPayloadValidator;
import com.learningdashboard.backend.rag.RetrievalService;
import com.learningdashboard.backend.storage.Artifact;
import com.learningdashboard.backend.storage.ArtifactStorage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The failsafe ladder end to end, with real validators and real fallback builders; only I/O is mocked. */
class VisualizePipelineTest {

    private static final String CONCEPT = "Photosynthesis\n1. Light hits the leaf\n2. Water splits\n3. Sugar is made";

    private static final String MIND_MAP_ANSWER = """
            { "title": "Photosynthesis", "summary": "Plants make sugar from light.", "suggestedFolder": "Biology",
              "visualization": { "type": "mind_map", "version": 1, "rootLabel": "Photosynthesis",
                "nodes": [{ "id": "n1", "label": "Light", "detail": "" }], "edges": [],
                "layoutHints": { "orientation": "radial", "rootNodeId": "n1" }, "citations": [] } }
            """;

    private static final String DIAGRAM_TWIN = """
            { "elements": [{ "id": "e1", "elementType": "process", "label": "Light hits leaf",
                              "x": 0, "y": 0, "width": 100, "height": 50, "style": "",
                              "accessibilityLabel": "Light hits the leaf" }], "connections": [] }
            """;

    private static String imageAnswer(String twinJson) {
        return """
                { "title": "Photosynthesis", "summary": "Plants make sugar from light.", "suggestedFolder": "",
                  "visualization": { "type": "image", "version": 1, "imagePrompt": "a leaf in sunlight",
                    "artifactId": "", "altText": "A leaf in sunlight"%s } }
                """.formatted(twinJson == null ? "" : ", \"fallbackDiagram\": " + twinJson);
    }

    private final TextGenerationProvider gemini = mock(TextGenerationProvider.class);
    private final TextGenerationProvider claude = mock(TextGenerationProvider.class);
    private final VisualGenerationProvider visualProvider = mock(VisualGenerationProvider.class);
    private final ArtifactStorage storage = mock(ArtifactStorage.class);
    private final RetrievalService retrieval = mock(RetrievalService.class);

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private VisualizePipeline pipeline;

    @BeforeEach
    void setUp() {
        // Same leniency Spring Boot's ObjectMapper has in production.
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ContentSafetyValidator safety = new ContentSafetyValidator(new ContentSafetyProperties());
        FallbackVisualizationService fallback = new FallbackVisualizationService(new ConceptTextOutliner(), safety,
                List.of(new MindMapFallbackBuilder(), new DiagramFallbackBuilder(), new AnimationFallbackBuilder()));
        TextGenerationProvider chain =
                new FallbackTextGenerationProvider(List.of(gemini, claude), new ProviderFallbackExecutor());
        pipeline = new VisualizePipeline(chain, new VisualPayloadValidator(), safety, retrieval,
                new VisualAssetGenerator(visualProvider, storage), fallback, mapper);
        when(retrieval.findRelated(anyString(), any(), anyInt())).thenReturn(List.of());
    }

    @Test
    void usesTheFirstProvidersAnswerWhenItIsGood() {
        when(gemini.generateText(anyString(), anyString())).thenReturn(MIND_MAP_ANSWER);

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "mind_map");

        assertThat(result.draft().title()).isEqualTo("Photosynthesis");
        assertThat(result.draft().suggestedFolder()).isEqualTo("Biology");
        assertThat(result.draft().payload().type()).isEqualTo("mind_map");
        assertThat(result.rawModelResponse()).isEqualTo(MIND_MAP_ANSWER);
    }

    @Test
    void anUnusableAnswerFromOneProviderFallsThroughToTheNext() {
        when(gemini.generateText(anyString(), anyString())).thenReturn("sorry, I can't do that");
        when(claude.generateText(anyString(), anyString())).thenReturn(MIND_MAP_ANSWER);

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "mind_map");

        assertThat(result.rawModelResponse()).isEqualTo(MIND_MAP_ANSWER);
    }

    @Test
    void aContentSafetyRejectionAlsoFallsThroughToTheNextProvider() {
        when(gemini.generateText(anyString(), anyString()))
                .thenReturn(MIND_MAP_ANSWER.replace("\"Photosynthesis\", \"summary\"", "\"<script>x</script>\", \"summary\""));
        when(claude.generateText(anyString(), anyString())).thenReturn(MIND_MAP_ANSWER);

        assertThat(pipeline.run(userId, jobId, CONCEPT, "mind_map").draft().title()).isEqualTo("Photosynthesis");
    }

    @Test
    void whenEveryProviderFailsTheVisualIsBuiltFromTheTextByRules() {
        when(gemini.generateText(anyString(), anyString())).thenThrow(new RuntimeException("503"));
        when(claude.generateText(anyString(), anyString())).thenThrow(new RuntimeException("no credit"));

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "auto");

        assertThat(result.rawModelResponse()).isNull();
        assertThat(result.draft().title()).isEqualTo("Photosynthesis");
        assertThat(result.draft().payload().type()).isEqualTo("animation"); // numbered steps
        assertThat(result.draft().artifactIds()).isEmpty();
    }

    @Test
    void whenEveryAnswerIsUnusableTheVisualIsBuiltFromTheTextByRules() {
        when(gemini.generateText(anyString(), anyString())).thenReturn("{ not json");
        when(claude.generateText(anyString(), anyString())).thenReturn("{\"title\": \"only a title\"}");

        assertThat(pipeline.run(userId, jobId, CONCEPT, "mind_map").draft().payload().type()).isEqualTo("mind_map");
    }

    @Test
    void theExplicitlyRequestedTypeIsKeptInTheRuleBasedFallback() {
        when(gemini.generateText(anyString(), anyString())).thenThrow(new RuntimeException("down"));
        when(claude.generateText(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        assertThat(pipeline.run(userId, jobId, CONCEPT, "image").draft().payload().type()).isEqualTo("diagram");
        assertThat(pipeline.run(userId, jobId, CONCEPT, "animation").draft().payload().type()).isEqualTo("animation");
    }

    @Test
    void imageFailureUsesTheAiWrittenDiagramAndKeepsTheAiTitle() {
        when(gemini.generateText(anyString(), anyString())).thenReturn(imageAnswer(DIAGRAM_TWIN));
        when(visualProvider.generateVisual(anyString())).thenThrow(new GenerationException(
                "All configured visual generation providers failed.", GenerationException.Code.ALL_PROVIDERS_FAILED));

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "image");

        assertThat(result.draft().payload().type()).isEqualTo("diagram");
        assertThat(result.draft().title()).isEqualTo("Photosynthesis");
        assertThat(result.draft().artifactIds()).isEmpty();
    }

    @Test
    void imageFailureWithNoUsableTwinBuildsTheDiagramFromTheText() {
        when(gemini.generateText(anyString(), anyString())).thenReturn(imageAnswer("{ \"elements\": [] }"));
        when(visualProvider.generateVisual(anyString())).thenThrow(new RuntimeException("timeout"));

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "image");

        assertThat(result.draft().payload().type()).isEqualTo("diagram");
        assertThat(result.draft().summary()).isEqualTo("Plants make sugar from light.");
    }

    @Test
    void imageSuccessCarriesTheDiagramTwin() {
        UUID artifactId = UUID.randomUUID();
        Artifact artifact = mock(Artifact.class);
        when(artifact.getId()).thenReturn(artifactId);
        when(gemini.generateText(anyString(), anyString())).thenReturn(imageAnswer(DIAGRAM_TWIN));
        when(visualProvider.generateVisual(anyString()))
                .thenReturn(new VisualGenerationProvider.VisualAsset(new byte[] {1}, "image/png"));
        when(storage.uploadGenerated(any(), anyString(), any(), any())).thenReturn(artifact);

        VisualizePipeline.Result result = pipeline.run(userId, jobId, CONCEPT, "image");

        ImagePayload image = (ImagePayload) result.draft().payload();
        assertThat(image.artifactId()).isEqualTo(artifactId.toString());
        assertThat(image.fallbackDiagram()).isNotNull();
        assertThat(image.fallbackDiagram().elements()).hasSize(1);
        assertThat(result.draft().artifactIds()).containsExactly(artifactId);
    }

    @Test
    void aRetrievalOutageDoesNotStopTheVisual() {
        when(retrieval.findRelated(anyString(), any(), anyInt())).thenThrow(new RuntimeException("embedding provider down"));
        when(gemini.generateText(anyString(), anyString())).thenReturn(MIND_MAP_ANSWER);

        assertThat(pipeline.run(userId, jobId, CONCEPT, "mind_map").draft().payload().type()).isEqualTo("mind_map");
    }
}
