package com.learningdashboard.backend.generation.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.learningdashboard.backend.rag.RelatedConcept;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisualizationPromptBuilderTest {

    @Test
    void wrapsConceptTextInDelimiterTags() {
        String userPrompt = new VisualizationPromptBuilder().withConceptText("photosynthesis explanation").buildUserPrompt();
        assertThat(userPrompt).contains("<concept_text>").contains("photosynthesis explanation").contains("</concept_text>");
    }

    @Test
    void omitsRelatedConceptsBlockWhenNoneRetrieved() {
        String userPrompt = new VisualizationPromptBuilder().withConceptText("x").withRelatedConcepts(List.of()).buildUserPrompt();
        assertThat(userPrompt).doesNotContain("previously saved");
    }

    @Test
    void includesRelatedConceptsWhenPresent() {
        List<RelatedConcept> related = List.of(new RelatedConcept(UUID.randomUUID(), "Cellular Respiration", "Breaks down glucose", 0.1));
        String userPrompt = new VisualizationPromptBuilder().withConceptText("x").withRelatedConcepts(related).buildUserPrompt();
        assertThat(userPrompt).contains("Cellular Respiration").contains("Breaks down glucose");
    }

    @Test
    void omitsPreferredTypeHintWhenAuto() {
        String userPrompt = new VisualizationPromptBuilder().withConceptText("x").withPreferredType("auto").buildUserPrompt();
        assertThat(userPrompt).doesNotContain("requested visualizationType");
    }

    @Test
    void includesPreferredTypeHintWhenSet() {
        String userPrompt = new VisualizationPromptBuilder().withConceptText("x").withPreferredType("diagram").buildUserPrompt();
        assertThat(userPrompt).contains("requested visualizationType").contains("diagram");
    }

    @Test
    void systemPromptDescribesAllFourVisualizationShapes() {
        String systemPrompt = new VisualizationPromptBuilder().buildSystemPrompt();
        assertThat(systemPrompt)
                .contains("\"mind_map\"")
                .contains("\"diagram\"")
                .contains("\"animation\"")
                .contains("\"image\"");
    }

    @Test
    void systemPromptDoesNotHardcodeASceneCount() {
        String systemPrompt = new VisualizationPromptBuilder().buildSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("decide the number of scenes");
    }
}
