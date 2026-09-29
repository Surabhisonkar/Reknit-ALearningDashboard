package com.learningdashboard.backend.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConceptSaveServiceTest {

    private final ConceptDraftSource draftSource = mock(ConceptDraftSource.class);
    private final ConceptService conceptService = mock(ConceptService.class);
    private final ConceptVersionWriter versionWriter = mock(ConceptVersionWriter.class);
    private final FolderAssigner folderAssigner = mock(FolderAssigner.class);
    private final ConceptSaveService service =
            new ConceptSaveService(draftSource, conceptService, versionWriter, folderAssigner);

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID biologyFolderId = UUID.randomUUID();
    private final UUID plantsFolderId = UUID.randomUUID();
    private final ConceptDraft draft = new ConceptDraft(jobId, null, "Photosynthesis", "summary", "Biology",
            "mind_map", "{\"type\":\"mind_map\",\"version\":1}", 1, List.of());

    @BeforeEach
    void wiring() {
        when(conceptService.save(any(Concept.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionWriter.write(any(Concept.class), anyInt(), any(ConceptDraft.class))).thenAnswer(inv -> inv.getArgument(0));
        when(draftSource.claim(jobId, userId)).thenReturn(new DraftClaim(draft, Optional.empty()));
        when(folderAssigner.findOrCreate(userId, "Biology")).thenReturn(Optional.of(biologyFolderId));
        when(folderAssigner.findOrCreate(userId, "Plants")).thenReturn(Optional.of(plantsFolderId));
    }

    private Concept existing(String title) {
        return new Concept(userId, title, "old", null, "mind_map", "{}", 1, null);
    }

    @Test
    void savesVersionOneAndMarksTheDraftSaved() {
        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, null, null, null));

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.concept().getTitle()).isEqualTo("Photosynthesis");
        assertThat(outcome.concept().getFolderId()).isEqualTo(biologyFolderId);
        verify(versionWriter).write(any(Concept.class), eq(1), eq(draft));
        verify(draftSource).markSaved(jobId, outcome.concept().getId());
    }

    @Test
    void rejectsADuplicateTitleBeforeWritingAnything() {
        Concept old = existing("photosynthesis");
        when(conceptService.findTitleCollision(userId, "Photosynthesis", null)).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.save(new SaveConceptCommand(jobId, userId, null, null, DuplicateTitlePolicy.REJECT)))
                .isInstanceOf(DuplicateTitleException.class)
                .extracting(e -> ((DuplicateTitleException) e).getDuplicateConceptId())
                .isEqualTo(old.getId());

        verify(conceptService, never()).save(any(Concept.class));
        verify(draftSource, never()).markSaved(any(), any());
    }

    @Test
    void replaceDeletesTheOldConceptAfterSavingTheNewOne() {
        Concept old = existing("Photosynthesis");
        when(conceptService.findTitleCollision(userId, "Photosynthesis", null)).thenReturn(Optional.of(old));

        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, null, null, DuplicateTitlePolicy.REPLACE));

        assertThat(outcome.created()).isTrue();
        verify(conceptService).deleteOwnedConcept(old.getId(), userId);
    }

    @Test
    void keepBothSavesWithoutDeleting() {
        when(conceptService.findTitleCollision(userId, "Photosynthesis", null)).thenReturn(Optional.of(existing("Photosynthesis")));

        service.save(new SaveConceptCommand(jobId, userId, null, null, DuplicateTitlePolicy.KEEP_BOTH));

        verify(conceptService, never()).deleteOwnedConcept(any(), any());
    }

    @Test
    void renameSavesUnderTheNewTitle() {
        when(conceptService.findTitleCollision(eq(userId), eq("Light reactions"), isNull())).thenReturn(Optional.empty());

        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, "  Light reactions ", null, null));

        assertThat(outcome.concept().getTitle()).isEqualTo("Light reactions");
        ArgumentCaptor<ConceptDraft> written = ArgumentCaptor.forClass(ConceptDraft.class);
        verify(versionWriter).write(any(Concept.class), eq(1), written.capture());
        assertThat(written.getValue().title()).isEqualTo("Light reactions");
    }

    @Test
    void folderOverrideWinsOverTheSuggestion() {
        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, null, "Plants", null));
        assertThat(outcome.concept().getFolderId()).isEqualTo(plantsFolderId);
    }

    @Test
    void aBlankFolderOverrideSavesTheConceptUnfiled() {
        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, null, "  ", null));
        assertThat(outcome.concept().getFolderId()).isNull();
        verify(folderAssigner, never()).findOrCreate(any(), any());
    }

    @Test
    void theFolderNameIsTrimmedBeforeTheLookup() {
        service.save(new SaveConceptCommand(jobId, userId, null, "  Plants  ", null));
        verify(folderAssigner).findOrCreate(userId, "Plants");
    }

    @Test
    void aRejectedDuplicateNeverCreatesAFolder() {
        when(conceptService.findTitleCollision(userId, "Photosynthesis", null)).thenReturn(Optional.of(existing("photosynthesis")));
        assertThatThrownBy(() -> service.save(new SaveConceptCommand(jobId, userId, null, null, DuplicateTitlePolicy.REJECT)))
                .isInstanceOf(DuplicateTitleException.class);
        verify(folderAssigner, never()).findOrCreate(any(), any());
    }

    @Test
    void savingTheSameDraftTwiceReturnsTheExistingConcept() {
        Concept saved = existing("Photosynthesis");
        when(draftSource.claim(jobId, userId)).thenReturn(new DraftClaim(null, Optional.of(saved.getId())));
        when(conceptService.requireOwnedConcept(saved.getId(), userId)).thenReturn(saved);

        SaveOutcome outcome = service.save(new SaveConceptCommand(jobId, userId, null, null, null));

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.concept()).isSameAs(saved);
        verify(versionWriter, never()).write(any(), anyInt(), any());
    }
}
