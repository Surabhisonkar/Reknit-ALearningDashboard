package com.learningdashboard.backend.generation.edit;

import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptDraft;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.concept.ConceptVersionWriter;
import com.learningdashboard.backend.generation.model.MindMapPayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import com.learningdashboard.backend.generation.validation.VisualPayloadValidator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Saves a user's own edit of a concept's visual as its next version. The
 * edit goes through the same checks as model output ({@link
 * VisualPayloadValidator}, {@link ContentSafetyValidator}) and is stored
 * through the same {@link ConceptVersionWriter} as a regenerate, so the
 * version history treats both alike. No model is called, and since the
 * title and summary don't change the embedding isn't recomputed either.
 */
@Service
public class VisualizationEditService {

    /** Types the frontend can edit and save. Diagram source edits could join later through the same path. */
    static final Set<String> EDITABLE_TYPES = Set.of("mind_map");

    private final ConceptService conceptService;
    private final ConceptVersionWriter versionWriter;
    private final VisualPayloadValidator payloadValidator;
    private final ContentSafetyValidator safetyValidator;
    private final ObjectMapper objectMapper;

    public VisualizationEditService(ConceptService conceptService, ConceptVersionWriter versionWriter,
                                    VisualPayloadValidator payloadValidator, ContentSafetyValidator safetyValidator,
                                    ObjectMapper objectMapper) {
        this.conceptService = conceptService;
        this.versionWriter = versionWriter;
        this.payloadValidator = payloadValidator;
        this.safetyValidator = safetyValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Concept saveEdit(UUID conceptId, UUID userId, JsonNode visualization) {
        // Row lock, like a regenerate: two saves at once get consecutive version numbers.
        Concept concept = conceptService.requireOwnedConceptForUpdate(conceptId, userId);

        String type = visualization.path("type").asText("");
        if (!EDITABLE_TYPES.contains(type)) {
            throw new InvalidVisualizationEditException("Only mind maps can be edited and saved.", List.of("type"));
        }
        if (!type.equals(concept.getVisualizationType())) {
            throw new InvalidVisualizationEditException("An edit can't change the visualization type.", List.of("type"));
        }
        int schemaVersion = visualization.path("version").asInt(1);
        if (schemaVersion < 2) {
            throw new InvalidVisualizationEditException("Edits must use visualization version 2.", List.of("version"));
        }

        String payloadJson = validatedJson(visualization);
        // An edit is still drawn from the same text, so it keeps the current version's source text.
        String sourceText = conceptService.requireVersion(conceptId, concept.getCurrentVersion()).getSourceText();
        ConceptDraft draft = new ConceptDraft(null, null, concept.getTitle(), concept.getSummary(), "",
                type, payloadJson, schemaVersion, List.of(), sourceText);
        return versionWriter.write(concept, concept.getCurrentVersion() + 1, draft, false);
    }

    /** Validates the edit and returns it re-serialized from the typed record, so only known fields are stored. */
    private String validatedJson(JsonNode visualization) {
        try {
            payloadValidator.validate(visualization);
            VisualizationPayload payload = objectMapper.convertValue(visualization, VisualizationPayload.class);
            if (payload instanceof MindMapPayload mindMap) {
                checkText(mindMap);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (GenerationException e) {
            throw new InvalidVisualizationEditException("This edit can't be saved.", detailsOf(e));
        } catch (JacksonException | IllegalArgumentException e) {
            throw new InvalidVisualizationEditException("This edit isn't a valid visualization.", List.of());
        }
    }

    private void checkText(MindMapPayload mindMap) {
        safetyValidator.validate("rootLabel", mindMap.rootLabel());
        for (MindMapPayload.Node node : mindMap.nodes()) {
            safetyValidator.validate("nodes[" + node.id() + "].label", node.label());
            safetyValidator.validate("nodes[" + node.id() + "].detail", node.detail());
        }
    }

    private static List<String> detailsOf(GenerationException e) {
        if (e.getDetails() instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
