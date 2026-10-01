package com.learningdashboard.backend.concept;

import com.learningdashboard.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Notes on a concept. Every method checks the concept belongs to the user
 * first (404 otherwise, like every concept route).
 *
 * Deliberately NOT {@code @Transactional}: {@link #addNote} is called from a
 * worker job handler, and an ownership failure thrown through a transactional
 * proxy would mark the job's transaction rollback-only, so the job couldn't
 * even be recorded as FAILED (handoff §4.1). The check throws before any
 * write, and each repository call is transactional on its own.
 */
@Service
public class ConceptNoteService {

    private final ConceptNoteRepository noteRepository;
    private final ConceptService conceptService;

    public ConceptNoteService(ConceptNoteRepository noteRepository, ConceptService conceptService) {
        this.noteRepository = noteRepository;
        this.conceptService = conceptService;
    }

    public ConceptNote addNote(UUID conceptId, UUID userId, String content, String source) {
        conceptService.requireOwnedConcept(conceptId, userId);
        return noteRepository.save(new ConceptNote(conceptId, userId, content, source));
    }

    /** Newest first. */
    public List<ConceptNote> listForConcept(UUID conceptId, UUID userId) {
        conceptService.requireOwnedConcept(conceptId, userId);
        return noteRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, userId);
    }

    public void deleteNote(UUID noteId, UUID conceptId, UUID userId) {
        ConceptNote note = noteRepository.findByIdAndConceptIdAndUserId(noteId, conceptId, userId)
                .orElseThrow(() -> new NotFoundException("Note not found."));
        noteRepository.delete(note);
    }
}
