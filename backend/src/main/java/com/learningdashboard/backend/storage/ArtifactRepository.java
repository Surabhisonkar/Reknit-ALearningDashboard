package com.learningdashboard.backend.storage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
    Optional<Artifact> findByIdAndUserId(UUID id, UUID userId);

    /** Feeds the scheduled cleanup job - orphaned artifacts past their expiry. */
    List<Artifact> findByExpiresAtBefore(Instant cutoff);
}
