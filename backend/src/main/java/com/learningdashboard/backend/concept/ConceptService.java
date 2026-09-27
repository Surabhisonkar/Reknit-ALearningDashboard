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
public class ConceptService {

    private static final String ANIMATION_TYPE = "animation";

    private final ConceptRepository conceptRepository;
    private final ConceptVersionRepository conceptVersionRepository;

    public ConceptService(ConceptRepository conceptRepository, ConceptVersionRepository conceptVersionRepository) {
        this.conceptRepository = conceptRepository;
        this.conceptVersionRepository = conceptVersionRepository;
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

    public List<Concept> listForUser(UUID userId, String folder) {
        return (folder == null || folder.isBlank())
                ? conceptRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : conceptRepository.findByUserIdAndFolderOrderByCreatedAtDesc(userId, folder);
    }

    @Transactional
    public void deleteOwnedConcept(UUID conceptId, UUID userId) {
        requireOwnedConcept(conceptId, userId); // 404s for both "doesn't exist" and "not yours"
        conceptRepository.deleteByIdAndUserId(conceptId, userId);
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
    public List<Concept> randomSparkFeed(UUID userId, String folder, Set<UUID> excludeIds, int limit) {
        List<Concept> pool = (folder == null || folder.isBlank())
                ? conceptRepository.findByUserIdAndVisualizationType(userId, ANIMATION_TYPE)
                : conceptRepository.findByUserIdAndFolderAndVisualizationType(userId, folder, ANIMATION_TYPE);

        List<Concept> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .filter(concept -> !excludeIds.contains(concept.getId()))
                .limit(limit)
                .toList();
    }

    public List<String> listFoldersForUser(UUID userId) {
        return conceptRepository.findDistinctNonEmptyFoldersByUserId(userId);
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
