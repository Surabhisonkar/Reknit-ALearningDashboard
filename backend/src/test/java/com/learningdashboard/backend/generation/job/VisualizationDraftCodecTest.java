package com.learningdashboard.backend.generation.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.concept.ConceptDraft;
import com.learningdashboard.backend.concept.VersionAppended;
import com.learningdashboard.backend.generation.model.ImagePayload;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisualizationDraftCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobJson jobJson = new JobJson(objectMapper);
    private final VisualizationDraftCodec codec = new VisualizationDraftCodec(jobJson);

    private final UUID artifactId = UUID.randomUUID();
    private final VisualizationDraft draft = new VisualizationDraft(
            "Photosynthesis", "Plants turn light into sugar.", "Biology",
            new ImagePayload(1, "a leaf in sunlight", artifactId.toString(), "A green leaf", null),
            List.of(artifactId));

    @Test
    void draftJsonCarriesEverythingTheFrontendNeeds() throws Exception {
        JsonNode json = objectMapper.readTree(codec.toDraftResultJson(draft));

        assertThat(json.path("kind").asText()).isEqualTo("DRAFT");
        assertThat(json.path("title").asText()).isEqualTo("Photosynthesis");
        assertThat(json.path("summary").asText()).isEqualTo("Plants turn light into sugar.");
        assertThat(json.path("suggestedFolder").asText()).isEqualTo("Biology");
        assertThat(json.path("visualizationType").asText()).isEqualTo("image");
        assertThat(json.path("visualization").path("type").asText()).isEqualTo("image");
        assertThat(json.path("visualization").path("artifactId").asText()).isEqualTo(artifactId.toString());
        assertThat(json.path("artifactIds").get(0).asText()).isEqualTo(artifactId.toString());
        assertThat(codec.isDraft(json)).isTrue();
    }

    @Test
    void readDraftRoundTripsTheStoredDraft() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID explainJobId = UUID.randomUUID();
        JsonNode stored = objectMapper.readTree(codec.toDraftResultJson(draft));

        ConceptDraft read = codec.readDraft(stored, jobId, explainJobId);

        assertThat(read.generationJobId()).isEqualTo(jobId);
        assertThat(read.sourceExplainJobId()).isEqualTo(explainJobId);
        assertThat(read.title()).isEqualTo("Photosynthesis");
        assertThat(read.visualizationType()).isEqualTo("image");
        assertThat(read.payloadSchemaVersion()).isEqualTo(1);
        assertThat(read.artifactIds()).containsExactly(artifactId);
        assertThat(objectMapper.readTree(read.visualizationPayloadJson()))
                .isEqualTo(stored.path("visualization"));
    }

    @Test
    void inMemoryConversionMatchesTheStoredForm() throws Exception {
        UUID jobId = UUID.randomUUID();
        ConceptDraft direct = codec.toConceptDraft(draft, jobId, null);
        ConceptDraft viaJson = codec.readDraft(objectMapper.readTree(codec.toDraftResultJson(draft)), jobId, null);

        assertThat(objectMapper.readTree(direct.visualizationPayloadJson()))
                .isEqualTo(objectMapper.readTree(viaJson.visualizationPayloadJson()));
        assertThat(direct.title()).isEqualTo(viaJson.title());
        assertThat(direct.artifactIds()).isEqualTo(viaJson.artifactIds());
    }

    @Test
    void versionResultKeepsTheExistingKeys() throws Exception {
        UUID duplicate = UUID.randomUUID();
        JsonNode json = objectMapper.readTree(codec.toVersionResultJson(new VersionAppended(UUID.randomUUID(), 3, duplicate)));

        assertThat(json.path("kind").asText()).isEqualTo("VERSION");
        assertThat(json.path("version").asInt()).isEqualTo(3);
        assertThat(json.path("duplicateTitleConceptId").asText()).isEqualTo(duplicate.toString());
        assertThat(codec.isDraft(json)).isFalse();
    }
}
