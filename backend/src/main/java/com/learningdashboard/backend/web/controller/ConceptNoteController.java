package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.ConceptNoteService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.ConceptNoteResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Notes saved on a concept, shown on Concept Detail (Phase 8). 404 for a concept or note that isn't yours. */
@RestController
@RequestMapping("/api/concepts/{id}/notes")
public class ConceptNoteController {

    private final ConceptNoteService noteService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    public ConceptNoteController(ConceptNoteService noteService, CurrentUserService currentUserService,
                                 ResponseMapper responseMapper) {
        this.noteService = noteService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public List<ConceptNoteResponse> list(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUser().getId();
        return noteService.listForConcept(id, userId).stream().map(responseMapper::toConceptNoteResponse).toList();
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @PathVariable UUID noteId) {
        noteService.deleteNote(noteId, id, currentUserService.requireCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }
}
