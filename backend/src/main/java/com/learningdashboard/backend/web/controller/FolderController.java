package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.folder.FolderDeletionMode;
import com.learningdashboard.backend.folder.FolderQueryService;
import com.learningdashboard.backend.folder.FolderService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.FolderCreateRequest;
import com.learningdashboard.backend.web.dto.FolderResponse;
import com.learningdashboard.backend.web.dto.FolderUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The user's folders ("playlists"). Every route is ownership-scoped; someone else's folder is a 404. */
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;
    private final FolderQueryService folderQueryService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    public FolderController(FolderService folderService, FolderQueryService folderQueryService,
                            CurrentUserService currentUserService, ResponseMapper responseMapper) {
        this.folderService = folderService;
        this.folderQueryService = folderQueryService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
    }

    /** Every folder, empty ones included, by name, each with its concept count. */
    @GetMapping
    public List<FolderResponse> list() {
        var user = currentUserService.requireCurrentUser();
        return folderQueryService.listForUser(user.getId()).stream()
                .map(responseMapper::toFolderResponse)
                .toList();
    }

    /** 201; 409 DUPLICATE_FOLDER_NAME if the name exists in any letter case. */
    @PostMapping
    public ResponseEntity<FolderResponse> create(@Valid @RequestBody FolderCreateRequest request) {
        var user = currentUserService.requireCurrentUser();
        var folder = folderService.createFolder(user.getId(), request.getName(), request.getColor());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseMapper.toFolderResponse(folderQueryService.summaryFor(folder)));
    }

    /** Rename and/or recolour; omitted fields stay as they are. */
    @PatchMapping("/{id}")
    public FolderResponse update(@PathVariable UUID id, @Valid @RequestBody FolderUpdateRequest request) {
        var user = currentUserService.requireCurrentUser();
        var folder = folderService.renameOrRecolor(id, user.getId(), request.getName(), request.getColor());
        return responseMapper.toFolderResponse(folderQueryService.summaryFor(folder));
    }

    /**
     * {@code ?concepts=unfile} keeps the folder's concepts (they become
     * unfiled); {@code ?concepts=delete} deletes them too. The parameter is
     * required - without it this is a 400, never a guess.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestParam(name = "concepts", required = false) String concepts) {
        var user = currentUserService.requireCurrentUser();
        folderService.deleteOwnedFolder(id, user.getId(), FolderDeletionMode.fromQueryValue(concepts));
        return ResponseEntity.noContent().build();
    }
}
