package com.learningdashboard.backend.rag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptEmbeddingRepository extends JpaRepository<ConceptEmbedding, UUID> {
    Optional<ConceptEmbedding> findByConceptId(UUID conceptId);

    void deleteByConceptId(UUID conceptId);

    /**
     * Feeds {@link RetrievalService}'s in-app similarity search. Loads
     * every embedding the user owns - fine at the per-user concept counts
     * this product expects (see the migration's comment on this table);
     * if that changes, this is the one method a future OpenSearch-backed
     * {@code RetrievalService} implementation would stop calling.
     */
    List<ConceptEmbedding> findByUserId(UUID userId);
}
