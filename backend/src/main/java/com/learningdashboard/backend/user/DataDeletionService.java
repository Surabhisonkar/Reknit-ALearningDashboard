package com.learningdashboard.backend.user;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.config.GdprProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GDPR-required data deletion, as a soft-delete-then-purge flow: a
 * deletion request sets {@code deletionRequestedAt} immediately (the
 * account stops being usable in practice — see {@code
 * CognitoUserProvisioningFilter}, which doesn't check this flag itself,
 * so pair this with a Cognito-side account disable if you want to block
 * login too, not just data retention), then a grace period gives the
 * user a window to change their mind before the hard purge runs.
 *
 * <p>The hard purge is a single {@code DELETE} on the {@code users} row
 * — every other table (concepts, generation_jobs, artifacts,
 * concept_embeddings) has {@code ON DELETE CASCADE} back to {@code
 * users.id} in the schema, so this one delete removes everything the
 * user owns. S3 objects are NOT deleted by this — {@code artifacts}
 * rows disappear, but the S3 bucket lifecycle policy (see infra/) is
 * what actually reclaims the underlying objects; this keeps the purge
 * fast and DB-transactional rather than making it also an S3-call loop
 * that could partially fail.
 */
@Service
public class DataDeletionService {

    private static final Logger log = LoggerFactory.getLogger(DataDeletionService.class);

    private final UserRepository userRepository;
    private final GdprProperties gdprProperties;

    public DataDeletionService(UserRepository userRepository, GdprProperties gdprProperties) {
        this.userRepository = userRepository;
        this.gdprProperties = gdprProperties;
    }

    @Transactional
    public Instant requestDeletion(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.getDeletionRequestedAt() == null) {
            user.setDeletionRequestedAt(Instant.now());
            userRepository.save(user);
        }

        return user.getDeletionRequestedAt().plus(gdprProperties.getGracePeriodDays(), ChronoUnit.DAYS);
    }

    @Transactional
    public void cancelDeletion(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        user.setDeletionRequestedAt(null);
        userRepository.save(user);
    }

    public boolean isDeletionPending(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getDeletionRequestedAt() != null)
                .orElse(false);
    }

    /**
     * Read-only - unlike {@link #requestDeletion}, never sets {@code
     * deletionRequestedAt} as a side effect. Returns {@code null} if no
     * deletion is currently pending for this user.
     */
    public Instant getDeletionEffectiveAt(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return user.getDeletionRequestedAt() == null
                ? null
                : user.getDeletionRequestedAt().plus(gdprProperties.getGracePeriodDays(), ChronoUnit.DAYS);
    }

    /** Called by {@link AccountPurgeJob}. Hard-deletes every user whose grace period has elapsed. */
    @Transactional
    public int purgeUsersPastGracePeriod() {
        Instant cutoff = Instant.now().minus(gdprProperties.getGracePeriodDays(), ChronoUnit.DAYS);
        List<User> toPurge = userRepository.findByDeletionRequestedAtBefore(cutoff);

        for (User user : toPurge) {
            log.info("Purging user {} - deletion requested {}, grace period elapsed.", user.getId(), user.getDeletionRequestedAt());
            userRepository.delete(user);
        }

        return toPurge.size();
    }
}
