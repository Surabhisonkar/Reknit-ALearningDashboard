package com.learningdashboard.backend.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import com.learningdashboard.backend.generation.prompt.VisualizationPromptBuilder;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import com.learningdashboard.backend.generation.validation.VisualPayloadValidator;
import com.learningdashboard.backend.rag.RelatedConcept;
import com.learningdashboard.backend.rag.RetrievalService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Backs "Visualize". Takes finished concept text (the user's own
 * explanation, or the AI's from the Explain step, whichever they picked)
 * and: retrieves RAG context server-side, asks the text provider to
 * structure it into one of the four rich visualization schemas,
 * validates that structure, and generates any per-scene/asset visuals
 * the model asked for. One pipeline, several distinct provider calls,
 * each doing the one thing it's good at - never a single call doing
 * explanation and visualization together.
 *
 * <p><b>Persists nothing.</b> The output is a {@link VisualizationDraft}
 * (generated assets are uploaded but left orphaned). Whether that draft
 * becomes the job's reviewable result (new concept) or is appended as a
 * new version (regenerate) is decided by {@code VisualizeJobHandler};
 * saving a new concept only ever happens on an explicit confirm-save
 * ({@code POST /api/concepts}).
 */
@Component
public class VisualizePipeline {

    private static final int RAG_TOP_K = 5;

    private final TextGenerationProvider textProvider;
    private final VisualPayloadValidator validator;
    private final ContentSafetyValidator contentSafetyValidator;
    private final RetrievalService retrievalService;
    private final VisualAssetGenerator assetGenerator;
    private final ObjectMapper objectMapper;

    public VisualizePipeline(TextGenerationProvider textProvider, VisualPayloadValidator validator,
                              ContentSafetyValidator contentSafetyValidator, RetrievalService retrievalService,
                              VisualAssetGenerator assetGenerator, ObjectMapper objectMapper) {
        this.textProvider = textProvider;
        this.validator = validator;
        this.contentSafetyValidator = contentSafetyValidator;
        this.retrievalService = retrievalService;
        this.assetGenerator = assetGenerator;
        this.objectMapper = objectMapper;
    }

    public Result run(UUID userId, UUID generationJobId, String conceptText, String preferredType) {

        List<RelatedConcept> related = retrievalService.findRelated(conceptText, userId, RAG_TOP_K);

        VisualizationPromptBuilder promptBuilder = new VisualizationPromptBuilder()
                .withConceptText(conceptText)
                .withRelatedConcepts(related)
                .withPreferredType(preferredType);

        String rawResponse = textProvider.generateText(promptBuilder.buildSystemPrompt(), promptBuilder.buildUserPrompt());
        JsonNode parsed = parseJsonSafely(rawResponse);

        String title = requireField(parsed, "title");
        String summary = requireField(parsed, "summary");
        contentSafetyValidator.validate("title", title);
        contentSafetyValidator.validate("summary", summary);
        String folder = parsed.path("suggestedFolder").asText("");
        JsonNode visualizationNode = parsed.path("visualization");

        validator.validate(visualizationNode);
        VisualizationPayload payload = objectMapper.convertValue(visualizationNode, VisualizationPayload.class);

        VisualAssetGenerator.Result assets = assetGenerator.generate(payload, userId, generationJobId);

        VisualizationDraft draft = new VisualizationDraft(title, summary, folder, assets.payload(), assets.artifactIds());
        return new Result(draft, rawResponse);
    }

    private String requireField(JsonNode parsed, String field) {
        String value = parsed.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new GenerationException(
                    "The AI response didn't match the expected structure.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, List.of(field + " is required"));
        }
        return value;
    }

    private JsonNode parseJsonSafely(String rawText) {
        try {
            String cleaned = rawText.trim().replaceFirst("^```json\\n?", "").replaceFirst("```$", "");
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new GenerationException("The AI response wasn't valid JSON.", GenerationException.Code.INVALID_JSON);
        }
    }

    public record Result(VisualizationDraft draft, String rawModelResponse) { }
}
