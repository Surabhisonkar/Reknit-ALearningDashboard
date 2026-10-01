package com.learningdashboard.backend.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.common.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConceptNoteServiceTest {

    private final ConceptNoteRepository repository = mock(ConceptNoteRepository.class);
    private final ConceptService conceptService = mock(ConceptService.class);
    private final ConceptNoteService service = new ConceptNoteService(repository, conceptService);
    private final UUID userId = UUID.randomUUID();
    private final UUID conceptId = UUID.randomUUID();

    @Test
    void addsANoteToAnOwnedConcept() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConceptNote note = service.addNote(conceptId, userId, "Condensed.", ConceptNote.SOURCE_CHAT);

        verify(conceptService).requireOwnedConcept(conceptId, userId);
        assertThat(note.getConceptId()).isEqualTo(conceptId);
        assertThat(note.getContent()).isEqualTo("Condensed.");
        assertThat(note.getSource()).isEqualTo("chat");
    }

    @Test
    void writesNothingForSomeoneElsesConcept() {
        when(conceptService.requireOwnedConcept(conceptId, userId)).thenThrow(new NotFoundException("Concept not found."));

        assertThatThrownBy(() -> service.addNote(conceptId, userId, "x", "chat")).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletingANoteThatIsntYoursIsNotFound() {
        UUID noteId = UUID.randomUUID();
        when(repository.findByIdAndConceptIdAndUserId(noteId, conceptId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNote(noteId, conceptId, userId)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
