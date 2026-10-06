package com.learningdashboard.backend.generation.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConceptTextOutlinerTest {

    private final ConceptTextOutliner outliner = new ConceptTextOutliner();

    @Test
    void usesAShortFirstLineAsTheTitle() {
        ConceptOutline outline = outliner.outline("Photosynthesis\n\nPlants turn light into sugar. It happens in the leaves.");
        assertThat(outline.title()).isEqualTo("Photosynthesis");
        assertThat(outline.points()).extracting(ConceptOutline.Point::detail)
                .containsExactly("Plants turn light into sugar.", "It happens in the leaves.");
    }

    @Test
    void numberedListIsSequentialAndKeepsItsOrder() {
        ConceptOutline outline = outliner.outline("Making tea\n1. Boil the water\n2. Add the tea leaves\n3. Wait three minutes");
        assertThat(outline.sequential()).isTrue();
        assertThat(outline.points()).extracting(ConceptOutline.Point::label)
                .containsExactly("Boil the water", "Add the tea leaves", "Wait three minutes");
    }

    @Test
    void stepWordsInProseAreSequential() {
        ConceptOutline outline = outliner.outline("First the seed absorbs water. Then a root appears. Finally a shoot grows.");
        assertThat(outline.sequential()).isTrue();
    }

    @Test
    void plainFactsAreNotSequential() {
        ConceptOutline outline = outliner.outline("Cells\n- Nucleus stores DNA\n- Mitochondria make energy");
        assertThat(outline.sequential()).isFalse();
    }

    @Test
    void headingsBecomeGroupsAsInTheFlattenedAiExplanation() {
        ConceptOutline outline = outliner.outline("""
                The Zeigarnik effect

                Unfinished tasks stay on your mind.

                Why it happens:
                - The brain keeps open loops active
                - Finishing releases the tension
                An example:
                A waiter remembers unpaid orders.
                """);
        assertThat(outline.title()).isEqualTo("The Zeigarnik effect");
        assertThat(outline.summary()).isEqualTo("Unfinished tasks stay on your mind.");
        assertThat(outline.points()).extracting(ConceptOutline.Point::group)
                .containsExactly("Why it happens", "Why it happens", "An example");
    }

    @Test
    void keyValueNotesUseTheKeyAsLabel() {
        ConceptOutline outline = outliner.outline("Plant parts\n- Chlorophyll: absorbs sunlight\n- Roots: take in water");
        assertThat(outline.points()).extracting(ConceptOutline.Point::label).containsExactly("Chlorophyll", "Roots");
    }

    @Test
    void longSentencesGetAShortLabelButKeepTheFullDetail() {
        String sentence = "The mitochondria convert the chemical energy stored in glucose into a form the cell can use.";
        ConceptOutline outline = outliner.outline("Energy\n" + sentence + "\nAnother fact here.");
        ConceptOutline.Point point = outline.points().get(0);
        assertThat(point.label().length()).isLessThanOrEqualTo(ConceptTextOutliner.MAX_LABEL);
        assertThat(point.label()).endsWith("…");
        assertThat(point.detail()).isEqualTo(sentence);
    }

    @Test
    void stripsControlCharactersAndMarkup() {
        ConceptOutline outline = outliner.outline("Topic\n<script>alert(1)</script> is text\u0000 here. Second point.");
        assertThat(outline.points()).allSatisfy(point -> {
            assertThat(point.detail()).doesNotContain("<").doesNotContain(">").doesNotContain("\u0000");
            assertThat(point.label()).doesNotContain("<");
        });
    }

    @Test
    void emptyOrNullTextStillYieldsAUsableOutline() {
        for (String text : new String[] {null, "", "   \n  "}) {
            ConceptOutline outline = outliner.outline(text);
            assertThat(outline.title()).isNotBlank();
            assertThat(outline.summary()).isNotBlank();
            assertThat(outline.points()).isNotEmpty();
        }
    }

    @Test
    void capsTheNumberOfPoints() {
        StringBuilder text = new StringBuilder("Big list\n");
        for (int i = 1; i <= 40; i++) {
            text.append("- item number ").append(i).append('\n');
        }
        assertThat(outliner.outline(text.toString()).points()).hasSize(ConceptTextOutliner.MAX_POINTS);
    }

    @Test
    void isDeterministic() {
        String text = "Topic\nFirst this happens. Then that happens.";
        assertThat(outliner.outline(text)).isEqualTo(outliner.outline(text));
    }
}
