package com.learningdashboard.backend.generation.fallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learningdashboard.backend.config.ContentSafetyProperties;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackVisualizationServiceTest {

    private static final String STEPS = "Making tea\n1. Boil the water\n2. Add the tea leaves\n3. Wait three minutes";
    private static final String FACTS = "Cells\n- Nucleus stores DNA\n- Mitochondria make energy";

    private static FallbackVisualizationService serviceWithDisallowedTerms(List<String> terms) {
        ContentSafetyProperties properties = new ContentSafetyProperties();
        properties.setDisallowedTerms(terms);
        return new FallbackVisualizationService(new ConceptTextOutliner(), new ContentSafetyValidator(properties),
                List.of(new MindMapFallbackBuilder(), new DiagramFallbackBuilder(), new AnimationFallbackBuilder()));
    }

    private final FallbackVisualizationService service = serviceWithDisallowedTerms(List.of());

    @Test
    void explicitTypesAreHonouredAndImageBecomesADiagram() {
        assertThat(service.buildDraft("mind_map", STEPS).payload().type()).isEqualTo("mind_map");
        assertThat(service.buildDraft("animation", FACTS).payload().type()).isEqualTo("animation");
        assertThat(service.buildDraft("diagram", FACTS).payload().type()).isEqualTo("diagram");
        assertThat(service.buildDraft("image", FACTS).payload().type()).isEqualTo("diagram");
    }

    @Test
    void autoPicksAnimationForStepsAndMindMapOtherwise() {
        assertThat(service.buildDraft("auto", STEPS).payload().type()).isEqualTo("animation");
        assertThat(service.buildDraft("auto", FACTS).payload().type()).isEqualTo("mind_map");
        assertThat(service.buildDraft(null, FACTS).payload().type()).isEqualTo("mind_map");
    }

    @Test
    void draftAlwaysHasTitleAndSummaryAndNoArtifacts() {
        VisualizationDraft draft = service.buildDraft("auto", FACTS);
        assertThat(draft.title()).isEqualTo("Cells");
        assertThat(draft.summary()).isNotBlank();
        assertThat(draft.artifactIds()).isEmpty();
    }

    @Test
    void disallowedTermInTheUsersOwnTitleIsReplacedNotFatal() {
        VisualizationDraft draft = serviceWithDisallowedTerms(List.of("forbidden"))
                .buildDraft("mind_map", "Forbidden topic\n- one point\n- another point");
        assertThat(draft.title()).isEqualTo(FallbackVisualizationService.NEUTRAL_TITLE);
    }

    @Test
    void rejectsTwoBuildersForTheSameType() {
        assertThatThrownBy(() -> new FallbackVisualizationService(new ConceptTextOutliner(),
                new ContentSafetyValidator(new ContentSafetyProperties()),
                List.of(new MindMapFallbackBuilder(), new MindMapFallbackBuilder())))
                .isInstanceOf(IllegalStateException.class);
    }
}
