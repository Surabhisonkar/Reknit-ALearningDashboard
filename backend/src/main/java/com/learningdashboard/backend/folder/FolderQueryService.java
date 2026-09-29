package com.learningdashboard.backend.folder;

import com.learningdashboard.backend.concept.ConceptFolderStats;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only questions about folders. Concept counts come through the concept module's published {@link ConceptFolderStats}. */
@Service
@Transactional(readOnly = true)
public class FolderQueryService {

    private final FolderRepository folderRepository;
    private final ConceptFolderStats conceptFolderStats;

    public FolderQueryService(FolderRepository folderRepository, ConceptFolderStats conceptFolderStats) {
        this.folderRepository = folderRepository;
        this.conceptFolderStats = conceptFolderStats;
    }

    /** Every folder (empty ones included), by name, with its concept count. */
    public List<FolderSummary> listForUser(UUID userId) {
        Map<UUID, Long> counts = conceptFolderStats.countByFolder(userId);
        return folderRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(folder -> new FolderSummary(folder, counts.getOrDefault(folder.getId(), 0L)))
                .toList();
    }

    /** The count for a single folder - used for the create/rename responses. */
    public FolderSummary summaryFor(Folder folder) {
        long count = conceptFolderStats.countByFolder(folder.getUserId()).getOrDefault(folder.getId(), 0L);
        return new FolderSummary(folder, count);
    }

    /** One query for a whole list of concepts' folder labels. */
    public Map<UUID, FolderLabel> labelsFor(UUID userId) {
        return folderRepository.findByUserIdOrderByNameAsc(userId).stream()
                .collect(Collectors.toMap(Folder::getId, FolderLabel::of, (a, b) -> a));
    }

    public Optional<FolderLabel> labelFor(UUID userId, UUID folderId) {
        return folderRepository.findByIdAndUserId(folderId, userId).map(FolderLabel::of);
    }

    /**
     * The legacy {@code GET /api/concepts/folders} list: names of folders that
     * hold at least one concept, by name - same as before folders were
     * entities, so Spark never offers an empty folder.
     */
    public List<String> namesWithConcepts(UUID userId) {
        Map<UUID, Long> counts = conceptFolderStats.countByFolder(userId);
        return folderRepository.findByUserIdOrderByNameAsc(userId).stream()
                .filter(folder -> counts.getOrDefault(folder.getId(), 0L) > 0)
                .map(Folder::getName)
                .toList();
    }

    public Optional<UUID> findIdByName(UUID userId, String rawName) {
        String name = Folder.normalizeName(rawName);
        return name.isEmpty() ? Optional.empty() : folderRepository.findByUserIdAndName(userId, name).map(Folder::getId);
    }

    public boolean isOwnedBy(UUID folderId, UUID userId) {
        return folderRepository.findByIdAndUserId(folderId, userId).isPresent();
    }
}
