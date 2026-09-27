package com.learningdashboard.backend.generation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.DraftClaim;
import com.learningdashboard.backend.concept.DraftNotSaveableException;
import com.learningdashboard.backend.generation.model.MindMapPayload;
import com.learningdashboard.backend.generation.model.VisualizationDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobConceptDraftSourceTest {

    private final GenerationJobRepository repository = mock(GenerationJobRepository.class);
    private final JobJson jobJson = new JobJson(new ObjectMapper());
    private final VisualizationDraftCodec codec = new VisualizationDraftCodec(jobJson);
    private final JobConceptDraftSource source = new JobConceptDraftSource(repository, codec, jobJson);

    private final UUID userId = UUID.randomUUID();

    private GenerationJob visualizeJob(String input) {
        GenerationJob job = new GenerationJob(userId, JobType.VISUALIZE, input);
        when(repository.lockByIdAndUserId(job.getId(), userId)).thenReturn(Optional.of(job));
        return job;
    }

    private String draftJson() {
        return codec.toDraftResultJson(new VisualizationDraft("T", "S", "",
                new MindMapPayload(1, "T", List.of(new MindMapPayload.Node("root", "T", "S")), List.of(),
                        new MindMapPayload.LayoutHints("radial", "root"), List.of()), List.of()));
    }

    @Test
    void unknownOrForeignJobIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.lockByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> source.claim(id, userId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unfinishedJobIsNotReady() {
        GenerationJob job = visualizeJob("{\"conceptText\":\"x\"}");
        assertThatThrownBy(() -> source.claim(job.getId(), userId))
                .isInstanceOf(DraftNotSaveableException.class)
                .extracting(e -> ((DraftNotSaveableException) e).getReason())
                .isEqualTo(DraftNotSaveableException.Reason.NOT_READY);
    }

    @Test
    void regenerateJobCannotBeSavedAsANewConcept() {
        GenerationJob job = visualizeJob("{\"conceptText\":\"x\",\"conceptId\":\"" + UUID.randomUUID() + "\"}");
        job.markCompleted("raw", "{\"kind\":\"VERSION\",\"version\":2}", UUID.randomUUID());
        assertThatThrownBy(() -> source.claim(job.getId(), userId)).isInstanceOf(DraftNotSaveableException.class);
    }

    @Test
    void completedDraftIsClaimable() {
        UUID explainJobId = UUID.randomUUID();
        GenerationJob job = visualizeJob("{\"conceptText\":\"x\",\"sourceExplainJobId\":\"" + explainJobId + "\"}");
        job.markCompleted("raw", draftJson(), null);

        DraftClaim claim = source.claim(job.getId(), userId);

        assertThat(claim.savedConceptId()).isEmpty();
        assertThat(claim.draft().title()).isEqualTo("T");
        assertThat(claim.draft().visualizationType()).isEqualTo("mind_map");
        assertThat(claim.draft().sourceExplainJobId()).isEqualTo(explainJobId);
        assertThat(claim.draft().generationJobId()).isEqualTo(job.getId());
    }

    @Test
    void alreadySavedDraftReportsItsConcept() {
        GenerationJob job = visualizeJob("{\"conceptText\":\"x\"}");
        job.markCompleted("raw", draftJson(), null);
        UUID conceptId = UUID.randomUUID();
        job.linkConcept(conceptId);

        assertThat(source.claim(job.getId(), userId).savedConceptId()).contains(conceptId);
    }
}
