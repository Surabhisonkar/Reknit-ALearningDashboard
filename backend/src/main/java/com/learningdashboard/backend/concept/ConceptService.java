package com.learningdashboard.backend.concept;

import com.learningdashboard.backend.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptService implements ConceptFolderContents {

    private static final String ANIMATION_TYPE = "animation";

    private final ConceptRepository conceptRepository;
    private final ConceptVersionRepository conceptVersionRepository;
    private final FolderLookup folderLookup;

    public ConceptService(ConceptRepository conceptRepository, ConceptVersionRepository conceptVersionRepository,
                          FolderLookup folderLookup) {
        this.conceptRepository = conceptRepository;
        this.conceptVersionRepository = conceptVersionRepository;
        this.folderLookup = folderLookup;
    }

    @Transactional
    public Concept save(Concept concept) {
        return conceptRepository.save(concept);
    }

    @Transactional
    public ConceptVersion saveVersion(ConceptVersion version) {
        return conceptVersionRepository.save(version);
    }

    /** Newest first - matches the order the frontend's version switcher shows them in. */
    public List<ConceptVersion> listVersions(UUID conceptId) {
        return conceptVersionRepository.findByConceptIdOrderByVersionDesc(conceptId);
    }

    /** Callers must have already checked ownership of the parent concept (e.g. via requireOwnedConcept) - a version has no owner of its own. */
    public ConceptVersion requireVersion(UUID conceptId, int version) {
        return conceptVersionRepository.findByConceptIdAndVersion(conceptId, version)
                .orElseThrow(() -> new NotFoundException("Version not found: " + version));
    }

    /**
     * Ownership is enforced in the query itself (findByIdAndUserId), not
     * as a separate "load then compare owner" step - that way there's no
     * window where a row briefly exists in memory without an ownership
     * check having been applied, and a cross-tenant request gets the same
     * 404 as a genuinely missing id (no existence leak).
     */
    public Concept requireOwnedConcept(UUID conceptId, UUID userId) {
        return conceptRepository.findByIdAndUserId(conceptId, userId)
                .orElseThrow(() -> new NotFoundException("Concept not found: " + conceptId));
    }

    /**
     * The user's concepts, newest first. {@code folderName} (optional) filters
     * by folder name, case-insensitively as before; an unknown name matches
     * nothing, exactly like the old string filter did.
     */
    public List<Concept> listForUser(UUID userId, String folderName) {
        if (folderName == null || folderName.isBlank()) {
            return conceptRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return folderLookup.findIdByName(userId, folderName)
                .map(folderId -> conceptRepository.findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId))
                .orElse(List.of());
    }

    @Transactional
    public void deleteOwnedConcept(UUID conceptId, UUID userId) {
        requireOwnedConcept(conceptId, userId); // 404s for both "doesn't exist" and "not yours"
        conceptRepository.deleteByIdAndUserId(conceptId, userId);
    }

    /**
     * Files an owned concept in one of the user's folders, or unfiles it
     * with {@code folderId == null}. Someone else's (or a missing) folder
     * 404s, the same as a missing concept - no existence leak.
     */
    @Transactional
    public Concept moveOwnedConcept(UUID conceptId, UUID userId, UUID folderId) {
        Concept concept = requireOwnedConcept(conceptId, userId);
        if (folderId != null && !folderLookup.isOwnedBy(folderId, userId)) {
            throw new NotFoundException("Folder not found: " + folderId);
        }
        concept.moveToFolder(folderId);
        return conceptRepository.save(concept);
    }

    /** {@inheritDoc} Rows cascade in the database: versions, embeddings and artifact rows go with each concept. */
    @Override
    @Transactional
    public int deleteAllInFolder(UUID userId, UUID folderId) {
        List<Concept> inFolder = conceptRepository.findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId);
        conceptRepository.deleteAll(inFolder);
        return inFolder.size();
    }

    @Transactional
    public Concept renameOwnedConcept(UUID conceptId, UUID userId, String newTitle) {
        Concept concept = requireOwnedConcept(conceptId, userId);
        concept.renameTo(newTitle);
        return conceptRepository.save(concept);
    }

    /**
     * Spark's feed: a random batch of the user's animation-type concepts,
     * optionally scoped to one folder ("a particular library"), excluding
     * whatever the client says it's already shown this session (so
     * "load more" doesn't repeat cards). Not persisted server-side as a
     * cursor - the client is the source of truth for what it's already
     * seen, which keeps this endpoint stateless like the rest of the API.
     */
    public List<Concept> randomSparkFeed(UUID userId, String folderName, Set<UUID> excludeIds, int limit) {
        List<Concept> pool;
        if (folderName == null || folderName.isBlank()) {
            pool = conceptRepository.findByUserIdAndVisualizationType(userId, ANIMATION_TYPE);
        } else {
            pool = folderLookup.findIdByName(userId, folderName)
                    .map(folderId -> conceptRepository.findByUserIdAndFolderIdAndVisualizationType(
                            userId, folderId, ANIMATION_TYPE))
                    .orElse(List.of());
        }

        List<Concept> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .filter(concept -> !excludeIds.contains(concept.getId()))
                .limit(limit)
                .toList();
    }

    /**
     * Same ownership semantics as {@link #requireOwnedConcept}, plus a row
     * lock held until the caller's transaction ends. Deliberately not
     * {@code @Transactional} itself: it must join the caller's
     * transaction, and a proxied not-found here would mark that whole
     * transaction rollback-only (the worker then couldn't record the job
     * as FAILED).
     */
    public Concept requireOwnedConceptForUpdate(UUID conceptId, UUID userId) {
        return conceptRepository.lockByIdAndUserId(conceptId, userId)
                .orElseThrow(() -> new NotFoundException("Concept not found: " + conceptId));
    }

    /**
     * Case-insensitive title collision among the user's concepts.
     *
     * @param excludeConceptId optional - a concept to ignore (itself, when checking after a regenerate)
     */
    public Optional<Concept> findTitleCollision(UUID userId, String title, UUID excludeConceptId) {
        return excludeConceptId == null
                ? conceptRepository.findFirstByUserIdAndTitleIgnoreCase(userId, title)
                : conceptRepository.findFirstByUserIdAndTitleIgnoreCaseAndIdNot(userId, title, excludeConceptId);
    }
}
