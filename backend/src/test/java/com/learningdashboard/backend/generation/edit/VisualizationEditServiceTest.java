package com.learningdashboard.backend.generation.edit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptDraft;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.concept.ConceptVersion;
import com.learningdashboard.backend.concept.ConceptVersionWriter;
import com.learningdashboard.backend.config.ContentSafetyProperties;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import com.learningdashboard.backend.generation.validation.VisualPayloadValidator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class VisualizationEditServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConceptService conceptService = mock(ConceptService.class);
    private final ConceptVersionWriter versionWriter = mock(ConceptVersionWriter.class);
    private final VisualizationEditService service = new VisualizationEditService(conceptService, versionWriter,
            new VisualPayloadValidator(), new ContentSafetyValidator(new ContentSafetyProperties()), mapper);

    private final UUID userId = UUID.randomUUID();

    private Concept mindMapConcept() {
        Concept concept = new Concept(userId, "Water cycle", "How water moves", null, "mind_map", "{}", 2, null);
        when(conceptService.requireOwnedConceptForUpdate(concept.getId(), userId)).thenReturn(concept);
        when(conceptService.requireVersion(concept.getId(), 1)).thenReturn(new ConceptVersion(
                concept.getId(), 1, "Water cycle", "How water moves", "mind_map", "{}", "Notes about the water cycle"));
        when(versionWriter.write(any(Concept.class), anyInt(), any(ConceptDraft.class), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(0));
        return concept;
    }

    private JsonNode mindMap(String rootLabel, String childParent) throws Exception {
        return mapper.readTree("""
                {"type":"mind_map","version":2,"rootLabel":"%s",
                 "nodes":[{"id":"root","label":"%s","detail":"","parentId":null},
                          {"id":"a","label":"Evaporation","detail":"Water rises","parentId":"%s"}],
                 "edges":[],"layoutHints":{"orientation":"horizontal","rootNodeId":"root"},"citations":[]}
                """.formatted(rootLabel, rootLabel, childParent));
    }

    @Test
    void savesAValidEditAsTheNextVersionWithoutReindexing() throws Exception {
        Concept concept = mindMapConcept();

        service.saveEdit(concept.getId(), userId, mindMap("The water cycle", "root"));

        ArgumentCaptor<ConceptDraft> draft = ArgumentCaptor.forClass(ConceptDraft.class);
        verify(versionWriter).write(eq(concept), eq(2), draft.capture(), eq(false));
        assertThat(draft.getValue().title()).isEqualTo("Water cycle");
        assertThat(draft.getValue().visualizationType()).isEqualTo("mind_map");
        assertThat(draft.getValue().visualizationPayloadJson()).contains("\"Evaporation\"");
        assertThat(draft.getValue().sourceText()).isEqualTo("Notes about the water cycle");
    }

    @Test
    void rejectsAnEditThatBreaksTheTree() throws Exception {
        Concept concept = mindMapConcept();

        assertThatThrownBy(() -> service.saveEdit(concept.getId(), userId, mindMap("The water cycle", "nowhere")))
                .isInstanceOf(InvalidVisualizationEditException.class);
        verify(versionWriter, never()).write(any(), anyInt(), any(), anyBoolean());
    }

    @Test
    void rejectsMarkupInLabels() throws Exception {
        Concept concept = mindMapConcept();

        assertThatThrownBy(() -> service.saveEdit(concept.getId(), userId, mindMap("<script>x</script>", "root")))
                .isInstanceOf(InvalidVisualizationEditException.class);
    }

    @Test
    void rejectsTypesThatAreNotEditable() throws Exception {
        Concept concept = mindMapConcept();
        JsonNode diagram = mapper.readTree("""
                {"type":"diagram","version":2,"syntax":"mermaid","source":"flowchart TD\\n a --> b","caption":""}
                """);

        assertThatThrownBy(() -> service.saveEdit(concept.getId(), userId, diagram))
                .isInstanceOf(InvalidVisualizationEditException.class);
    }
}
