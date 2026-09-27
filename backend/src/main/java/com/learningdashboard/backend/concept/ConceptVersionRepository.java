package com.learningdashboard.backend.concept;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptVersionRepository extends JpaRepository<ConceptVersion, UUID> {
    List<ConceptVersion> findByConceptIdOrderByVersionDesc(UUID conceptId);
    Optional<ConceptVersion> findByConceptIdAndVersion(UUID conceptId, int version);
}
