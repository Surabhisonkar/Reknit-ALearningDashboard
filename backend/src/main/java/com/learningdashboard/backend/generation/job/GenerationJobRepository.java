package com.learningdashboard.backend.generation.job;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    Optional<GenerationJob> findByIdAndUserId(UUID id, UUID userId);

    /** Ownership-checked load with a row lock - confirm-save uses it so two saves of one draft serialize. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from GenerationJob j where j.id = :id and j.userId = :userId")
    Optional<GenerationJob> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /** The most recent job of a type linked to a concept - post-save regenerate reuses its original input. */
    Optional<GenerationJob> findFirstByUserIdAndConceptIdAndJobTypeOrderByCreatedAtDesc(UUID userId, UUID conceptId, JobType jobType);
}
