package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.folder.FolderQueryService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ConceptResponseAssembler;
import com.learningdashboard.backend.web.dto.ConceptFolderMoveRequest;
import com.learningdashboard.backend.web.dto.ConceptResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Concept routes that are about folders. Kept out of {@code ConceptController}
 * (single responsibility); Spring routes these literal sub-paths alongside
 * that controller's {@code /{id}} routes without conflict.
 */
@RestController
@RequestMapping("/api/concepts")
public class ConceptFolderController {

    private final ConceptService conceptService;
    private final FolderQueryService folderQueryService;
    private final ConceptResponseAssembler assembler;
    private final CurrentUserService currentUserService;

    public ConceptFolderController(ConceptService conceptService, FolderQueryService folderQueryService,
                                   ConceptResponseAssembler assembler, CurrentUserService currentUserService) {
        this.conceptService = conceptService;
        this.folderQueryService = folderQueryService;
        this.assembler = assembler;
        this.currentUserService = currentUserService;
    }

    /** Files a concept in one of the user's folders, or unfiles it ({@code folderId: null}). 404 for someone else's concept or folder. */
    @PutMapping("/{id}/folder")
    public ConceptResponse move(@PathVariable UUID id, @RequestBody ConceptFolderMoveRequest request) {
        var user = currentUserService.requireCurrentUser();
        return assembler.toResponse(conceptService.moveOwnedConcept(id, user.getId(), request.getFolderId()));
    }

    /**
     * Names of folders holding at least one concept - the same contract as
     * before folders were entities (it used to live on ConceptController).
     * Powers Spark's folder picker.
     */
    @GetMapping("/folders")
    public List<String> folderNames() {
        var user = currentUserService.requireCurrentUser();
        return folderQueryService.namesWithConcepts(user.getId());
    }
}
