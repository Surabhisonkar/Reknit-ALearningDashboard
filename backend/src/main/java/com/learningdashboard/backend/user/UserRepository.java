package com.learningdashboard.backend.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCognitoSub(String cognitoSub);

    /** Feeds the GDPR purge job - users whose grace period has elapsed. */
    List<User> findByDeletionRequestedAtBefore(Instant cutoff);
}
