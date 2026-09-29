package com.learningdashboard.backend.folder;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.ConceptFolderContents;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every change to folders: create, rename/recolour, delete, and find-or-create for auto-filing. Reads live in {@link FolderQueryService}. */
@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderColorAssigner colorAssigner;
    private final ConceptFolderContents conceptFolderContents;

    public FolderService(FolderRepository folderRepository, FolderColorAssigner colorAssigner,
                         ConceptFolderContents conceptFolderContents) {
        this.folderRepository = folderRepository;
        this.colorAssigner = colorAssigner;
        this.conceptFolderContents = conceptFolderContents;
    }

    /** {@code color == null} means "pick one for me" (least-used palette colour). */
    @Transactional
    public Folder createFolder(UUID userId, String rawName, FolderColor color) {
        String name = requireName(rawName);
        if (folderRepository.existsByUserIdAndName(userId, name)) {
            throw new DuplicateFolderNameException(name);
        }
        Folder folder = new Folder(userId, name, color != null ? color : colorAssigner.nextColor(userId));
        return saveGuardingUniqueName(folder, name);
    }

    /** Either argument may be null to leave it unchanged. Renaming to a different letter case of the same name is allowed. */
    @Transactional
    public Folder renameOrRecolor(UUID folderId, UUID userId, String rawName, FolderColor color) {
        Folder folder = requireOwnedFolder(folderId, userId);
        if (rawName != null) {
            String name = requireName(rawName);
            if (folderRepository.existsByUserIdAndNameAndIdNot(userId, name, folderId)) {
                throw new DuplicateFolderNameException(name);
            }
            folder.rename(name);
        }
        if (color != null) {
            folder.recolor(color);
        }
        return saveGuardingUniqueName(folder, folder.getName());
    }

    /**
     * Deletes a folder. {@link FolderDeletionMode#UNFILE} keeps its concepts
     * (the FK sets their folder_id to NULL); {@link FolderDeletionMode#DELETE_CONCEPTS}
     * deletes them first, in this same transaction - all or nothing.
     */
    @Transactional
    public void deleteOwnedFolder(UUID folderId, UUID userId, FolderDeletionMode mode) {
        Folder folder = requireOwnedFolder(folderId, userId);
        if (mode == FolderDeletionMode.DELETE_CONCEPTS) {
            conceptFolderContents.deleteAllInFolder(userId, folderId);
        }
        folderRepository.delete(folder);
    }

    /**
     * Auto-filing: the user's folder with this name (case-insensitive), created
     * with an auto colour if missing. Race-safe inside the caller's transaction:
     * lock-read, insert-if-absent (a no-op if a concurrent save just created it),
     * lock-read again. Empty for a blank name.
     */
    @Transactional
    public Optional<Folder> findOrCreateByName(UUID userId, String rawName) {
        String name = Folder.normalizeName(rawName);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        Optional<Folder> existing = folderRepository.lockByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            return existing;
        }
        folderRepository.insertIfAbsent(UUID.randomUUID().toString(), userId.toString(), name,
                colorAssigner.nextColor(userId).key(), Instant.now());
        return folderRepository.lockByUserIdAndName(userId, name);
    }

    /** Same 404 for "doesn't exist" and "not yours" - no existence leak. */
    public Folder requireOwnedFolder(UUID folderId, UUID userId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new NotFoundException("Folder not found: " + folderId));
    }

    private static String requireName(String rawName) {
        String name = Folder.normalizeName(rawName);
        if (name.isEmpty()) {
            throw new InvalidFolderRequestException("Folder name is required.");
        }
        return name;
    }

    /** The existence check covers the normal case; the unique key still catches a concurrent create of the same name. */
    private Folder saveGuardingUniqueName(Folder folder, String name) {
        try {
            return folderRepository.saveAndFlush(folder);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFolderNameException(name);
        }
    }
}
