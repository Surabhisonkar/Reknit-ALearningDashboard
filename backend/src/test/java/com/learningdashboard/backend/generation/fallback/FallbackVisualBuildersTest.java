package com.learningdashboard.backend.generation.fallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.generation.model.AnimationPayload;
import com.learningdashboard.backend.generation.model.DiagramPayload;
import com.learningdashboard.backend.generation.model.MindMapPayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import com.learningdashboard.backend.generation.validation.VisualPayloadValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every builder's output must pass the same validator the AI's output
 * does, for small, typical and oversized inputs - a failsafe that can be
 * rejected downstream isn't one.
 */
class FallbackVisualBuildersTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final VisualPayloadValidator validator = new VisualPayloadValidator();
    private final ConceptTextOutliner outliner = new ConceptTextOutliner();
    private final List<FallbackVisualBuilder> builders =
            List.of(new MindMapFallbackBuilder(), new DiagramFallbackBuilder(), new AnimationFallbackBuilder());

    private static final String GROUPED = """
            The Zeigarnik effect

            Unfinished tasks stay on your mind.

            Why it happens:
            - The brain keeps open loops active
            - Finishing releases the tension
            How to use it:
            - Start a task before a break
            - If you feel stuck, write the next step down
            """;

    private static String oversized() {
        StringBuilder text = new StringBuilder("A very long list\n");
        for (int i = 1; i <= 60; i++) {
            text.append(i).append(". ").append("Step ").append(i).append(' ').append("word ".repeat(120)).append('\n');
        }
        return text.toString();
    }

    @Test
    void everyBuilderOutputPassesTheValidator() {
        for (String text : new String[] {null, "", "One line only", "Tea\n1. Boil water\n2. Add leaves", GROUPED, oversized()}) {
            ConceptOutline outline = outliner.outline(text);
            for (FallbackVisualBuilder builder : builders) {
                VisualizationPayload payload = builder.build(outline);
                JsonNode json = mapper.valueToTree(payload);
                assertThat(payload.type()).isEqualTo(builder.type());
                assertThatCode(() -> validator.validate(json))
                        .as("%s for input starting '%s'", builder.type(), text == null ? null : text.lines().findFirst().orElse(""))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    void mindMapBranchesByHeadingAndConnectsEveryNode() {
        MindMapPayload map = (MindMapPayload) new MindMapFallbackBuilder().build(outliner.outline(GROUPED));

        assertThat(map.rootLabel()).isEqualTo("The Zeigarnik effect");
        assertThat(map.layoutHints().rootNodeId()).isEqualTo(MindMapFallbackBuilder.ROOT_ID);
        assertThat(map.nodes()).extracting(MindMapPayload.Node::label).contains("Why it happens", "How to use it");
        // a tree: every node except the root has exactly one incoming edge
        assertThat(map.edges()).hasSize(map.nodes().size() - 1);
        assertThat(map.edges()).extracting(MindMapPayload.Edge::targetId).doesNotHaveDuplicates();
    }

    @Test
    void mindMapWithoutHeadingsIsOneRingAroundTheRoot() {
        MindMapPayload map = (MindMapPayload) new MindMapFallbackBuilder()
                .build(outliner.outline("Cells\n- Nucleus stores DNA\n- Mitochondria make energy"));
        assertThat(map.edges()).allSatisfy(edge -> assertThat(edge.sourceId()).isEqualTo(MindMapFallbackBuilder.ROOT_ID));
        assertThat(map.nodes()).hasSize(3);
    }

    @Test
    void diagramIsAChainFromTheTitleWithConditionsAsDecisions() {
        DiagramPayload diagram = (DiagramPayload) new DiagramFallbackBuilder().build(outliner.outline(GROUPED));

        assertThat(diagram.elements().get(0).elementType()).isEqualTo("terminator");
        assertThat(diagram.elements().get(0).label()).isEqualTo("The Zeigarnik effect");
        assertThat(diagram.connections()).hasSize(diagram.elements().size() - 1);
        assertThat(diagram.elements()).filteredOn(el -> el.elementType().equals("note"))
                .extracting(DiagramPayload.Element::label).containsExactly("Why it happens", "How to use it");
        assertThat(diagram.elements()).filteredOn(el -> el.elementType().equals("decision")).hasSize(1);
    }

    @Test
    void animationHasOneSceneForEachStepInOrderWithNoImageRequests() {
        AnimationPayload animation = (AnimationPayload) new AnimationFallbackBuilder()
                .build(outliner.outline("Making tea\n1. Boil the water\n2. Add the tea leaves\n3. Wait three minutes"));

        assertThat(animation.scenes()).extracting(AnimationPayload.Scene::title)
                .containsExactly("Boil the water", "Add the tea leaves", "Wait three minutes");
        assertThat(animation.scenes()).extracting(AnimationPayload.Scene::order).containsExactly(0, 1, 2);
        assertThat(animation.scenes()).allSatisfy(scene -> {
            assertThat(scene.needsVisualAsset()).isFalse();
            assertThat(scene.assetArtifactIds()).isEmpty();
            assertThat(scene.durationSeconds()).isBetween(3.0, 12.0);
        });
    }

    @Test
    void animationScenesWithMoreWordsRunLonger() {
        assertThat(AnimationFallbackBuilder.durationFor("word ".repeat(20)))
                .isGreaterThan(AnimationFallbackBuilder.durationFor("two words"));
    }
}
