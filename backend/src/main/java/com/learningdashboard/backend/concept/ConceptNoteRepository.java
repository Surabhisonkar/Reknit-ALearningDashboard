package com.learningdashboard.backend.concept;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptNoteRepository extends JpaRepository<ConceptNote, UUID> {

    List<ConceptNote> findByConceptIdAndUserIdOrderByCreatedAtDesc(UUID conceptId, UUID userId);

    Optional<ConceptNote> findByIdAndConceptIdAndUserId(UUID id, UUID conceptId, UUID userId);
}
