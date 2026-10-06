package com.learningdashboard.backend.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.generation.fallback.FallbackVisualizationService;
import com.learningdashboard.backend.generation.model.ImagePayload;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><b>Failsafe - the user always gets a visual.</b> In order:
 * <ol>
 *   <li>Every configured text provider is tried in turn; an answer that
 *   is invalid JSON, the wrong structure, or rejected by content safety
 *   counts as that provider failing, and the next one is asked.</li>
 *   <li>If the picture for an image cannot be produced, the flowchart the
 *   AI wrote alongside it is used (see {@link VisualAssetGenerator}).</li>
 *   <li>If no provider produced anything usable, {@link
 *   FallbackVisualizationService} builds the visual from the user's own
 *   text by rules, with no AI at all.</li>
 * </ol>
 * Only a failure of that last step fails the job; it is logged at ERROR
 * here and recorded on the job row by {@code JobProcessingService}.
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

    private static final Logger log = LoggerFactory.getLogger(VisualizePipeline.class);

    private static final int RAG_TOP_K = 5;
    private static final String FALLBACK_DIAGRAM = "fallbackDiagram";

    private final TextGenerationProvider textProvider;
    private final VisualPayloadValidator validator;
    private final ContentSafetyValidator contentSafetyValidator;
    private final RetrievalService retrievalService;
    private final VisualAssetGenerator assetGenerator;
    private final FallbackVisualizationService fallbackService;
    private final ObjectMapper objectMapper;

    public VisualizePipeline(TextGenerationProvider textProvider, VisualPayloadValidator validator,
                              ContentSafetyValidator contentSafetyValidator, RetrievalService retrievalService,
                              VisualAssetGenerator assetGenerator, FallbackVisualizationService fallbackService,
                              ObjectMapper objectMapper) {
        this.textProvider = textProvider;
        this.validator = validator;
        this.contentSafetyValidator = contentSafetyValidator;
        this.retrievalService = retrievalService;
        this.assetGenerator = assetGenerator;
        this.fallbackService = fallbackService;
        this.objectMapper = objectMapper;
    }

    public Result run(UUID userId, UUID generationJobId, String conceptText, String preferredType) {
        VisualizationPromptBuilder promptBuilder = new VisualizationPromptBuilder()
                .withConceptText(conceptText)
                .withRelatedConcepts(findRelatedOrNone(conceptText, userId, generationJobId))
                .withPreferredType(preferredType);

        Structured structured;
        try {
            structured = textProvider.generateAndParse(
                    promptBuilder.buildSystemPrompt(), promptBuilder.buildUserPrompt(), this::parseAndValidate);
        } catch (RuntimeException e) {
            log.warn("Job {}: no AI provider produced a usable visualization ({}), building one from the "
                    + "concept text by rules.", generationJobId, describe(e));
            return new Result(buildRuleBasedDraft(generationJobId, preferredType, conceptText), null);
        }

        VisualizationPayload payload;
        List<UUID> artifactIds;
        try {
            VisualAssetGenerator.Result assets = assetGenerator.generate(structured.payload(), userId, generationJobId);
            payload = assets.payload();
            artifactIds = assets.artifactIds();
        } catch (RuntimeException e) {
            // Only reachable for an image with no AI-written flowchart to fall back on.
            log.warn("Job {}: visual assets could not be generated ({}), building a flowchart from the "
                    + "concept text by rules.", generationJobId, describe(e));
            payload = buildRuleBasedPayload(generationJobId, standInTypeFor(structured.payload()), conceptText);
            artifactIds = List.of();
        }

        VisualizationDraft draft = new VisualizationDraft(
                structured.title(), structured.summary(), structured.folder(), payload, artifactIds);
        return new Result(draft, structured.rawResponse());
    }

    // ---- AI track ---------------------------------------------------------

    /** Throws if this provider's answer is unusable, which sends the chain on to the next provider. */
    private Structured parseAndValidate(String rawResponse) {
        JsonNode parsed = parseJsonSafely(rawResponse);

        String title = requireField(parsed, "title");
        String summary = requireField(parsed, "summary");
        contentSafetyValidator.validate("title", title);
        contentSafetyValidator.validate("summary", summary);
        String folder = parsed.path("suggestedFolder").asText("");
        JsonNode visualizationNode = parsed.path("visualization");

        normalizeFallbackDiagram(visualizationNode);
        validator.validate(visualizationNode);
        VisualizationPayload payload;
        try {
            payload = objectMapper.convertValue(visualizationNode, VisualizationPayload.class);
        } catch (IllegalArgumentException e) {
            throw new GenerationException(
                    "The AI response didn't match the expected visualization structure.",
                    GenerationException.Code.SCHEMA_VALIDATION_FAILED, List.of(String.valueOf(e.getMessage())));
        }
        return new Structured(title, summary, folder, payload, rawResponse);
    }

    /**
     * An image's flowchart twin is a bonus, not a requirement: a missing
     * "type" is filled in, and a malformed twin is dropped rather than
     * rejecting an otherwise good image answer.
     */
    private void normalizeFallbackDiagram(JsonNode visualizationNode) {
        if (!(visualizationNode instanceof ObjectNode visualization)
                || !"image".equals(visualization.path("type").asText(null))) {
            return;
        }
        JsonNode twin = visualization.path(FALLBACK_DIAGRAM);
        if (twin instanceof ObjectNode diagram) {
            diagram.put("type", "diagram");
            if (!diagram.path("version").isInt()) {
                diagram.put("version", 1);
            }
            try {
                validator.validate(diagram);
                return;
            } catch (GenerationException invalid) {
                log.warn("Dropping a malformed fallbackDiagram from an image answer: {}", invalid.getDetails());
            }
        }
        visualization.remove(FALLBACK_DIAGRAM);
    }

    /** RAG context is a nice-to-have; an embedding outage must not cost the user their visual. */
    private List<RelatedConcept> findRelatedOrNone(String conceptText, UUID userId, UUID generationJobId) {
        try {
            return retrievalService.findRelated(conceptText, userId, RAG_TOP_K);
        } catch (RuntimeException e) {
            log.warn("Job {}: related-concept retrieval failed ({}), continuing without it.",
                    generationJobId, describe(e));
            return List.of();
        }
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

    // ---- rule-based track (no AI) ------------------------------------------

    private VisualizationDraft buildRuleBasedDraft(UUID generationJobId, String preferredType, String conceptText) {
        try {
            return fallbackService.buildDraft(preferredType, conceptText);
        } catch (RuntimeException e) {
            log.error("Job {}: the rule-based failsafe visual could not be built either.", generationJobId, e);
            throw e;
        }
    }

    private VisualizationPayload buildRuleBasedPayload(UUID generationJobId, String type, String conceptText) {
        try {
            return fallbackService.buildPayload(type, conceptText);
        } catch (RuntimeException e) {
            log.error("Job {}: the rule-based failsafe {} could not be built either.", generationJobId, type, e);
            throw e;
        }
    }

    /** An image has no coded equivalent, so a flowchart stands in for it; other types stand in for themselves. */
    private String standInTypeFor(VisualizationPayload payload) {
        return payload instanceof ImagePayload ? "diagram" : payload.type();
    }

    /** The root cause's message when there is one - "All providers failed" alone says nothing useful in a log. */
    private String describe(RuntimeException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        Object details = cause instanceof GenerationException generation ? generation.getDetails() : null;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage() + (details != null ? " " + details : "");
    }

    private record Structured(String title, String summary, String folder, VisualizationPayload payload,
                              String rawResponse) { }

    /** {@code rawModelResponse} is null when the draft was built by rules and no model answer was used. */
    public record Result(VisualizationDraft draft, String rawModelResponse) { }
}
