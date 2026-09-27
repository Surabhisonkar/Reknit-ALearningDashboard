package com.learningdashboard.backend.concept;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConceptRepository extends JpaRepository<Concept, UUID> {
    Optional<Concept> findByIdAndUserId(UUID id, UUID userId);

    List<Concept> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Concept> findByUserIdAndFolderOrderByCreatedAtDesc(UUID userId, String folder);

    void deleteByIdAndUserId(UUID id, UUID userId);

    /** Used by the GDPR account-deletion job; concepts cascade-delete with the user otherwise too. */
    void deleteAllByUserId(UUID userId);

    /** Feeds the duplicate-name check after a new concept is saved - excludes itself, case-insensitive. */
    Optional<Concept> findFirstByUserIdAndTitleIgnoreCaseAndIdNot(UUID userId, String title, UUID excludeId);

    /**
     * Spark's candidate pool: every one of the user's concepts of a given
     * visualization type (Spark only ever plays "animation" concepts).
     * Fetched whole and shuffled in-app rather than via {@code ORDER BY
     * RAND()} - same documented "fine at personal-library scale" trade-off
     * as {@code RetrievalService}, and it sidesteps DB-specific random-
     * function differences and empty-IN-list edge cases entirely.
     */
    List<Concept> findByUserIdAndVisualizationType(UUID userId, String visualizationType);

    List<Concept> findByUserIdAndFolderAndVisualizationType(UUID userId, String folder, String visualizationType);

    /** Powers the "which library?" picker on Spark - every distinct non-empty folder name the user has used. */
    @Query("select distinct c.folder from Concept c where c.userId = :userId and c.folder <> '' order by c.folder")
    List<String> findDistinctNonEmptyFoldersByUserId(UUID userId);

    /** Confirm-save's duplicate check - runs before the new concept exists, so there's nothing to exclude. */
    Optional<Concept> findFirstByUserIdAndTitleIgnoreCase(UUID userId, String title);

    /** Ownership-checked load that also takes a row lock for the rest of the transaction (serializes concurrent regenerates). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Concept c where c.id = :id and c.userId = :userId")
    Optional<Concept> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
