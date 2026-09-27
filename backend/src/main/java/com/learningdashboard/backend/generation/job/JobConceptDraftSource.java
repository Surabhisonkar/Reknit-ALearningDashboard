package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.ConceptDraft;
import com.learningdashboard.backend.concept.ConceptDraftSource;
import com.learningdashboard.backend.concept.DraftClaim;
import com.learningdashboard.backend.concept.DraftNotSaveableException;
import com.learningdashboard.backend.concept.DraftNotSaveableException.Reason;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter: drafts live on completed VISUALIZE job rows (Handoff Part 5 -
 * "draft = job result, nothing more"), so no separate drafts table exists.
 * Implements the concept module's {@link ConceptDraftSource} port.
 */
@Component
public class JobConceptDraftSource implements ConceptDraftSource {

    /**
     * Must stay below the artifact orphan TTL (24h in {@code
     * S3ArtifactStorage}): past that, a draft's generated images may
     * already have been removed by the bucket lifecycle rule, so saving
     * it would produce a concept with broken images.
     */
    static final Duration DRAFT_LIFETIME = Duration.ofHours(23);

    private final GenerationJobRepository jobRepository;
    private final VisualizationDraftCodec codec;
    private final JobJson jobJson;

    public JobConceptDraftSource(GenerationJobRepository jobRepository, VisualizationDraftCodec codec, JobJson jobJson) {
        this.jobRepository = jobRepository;
        this.codec = codec;
        this.jobJson = jobJson;
    }

    @Override
    public DraftClaim claim(UUID draftId, UUID userId) {
        GenerationJob job = jobRepository.lockByIdAndUserId(draftId, userId)
                .orElseThrow(() -> new NotFoundException("Draft not found: " + draftId));

        if (job.getJobType() != JobType.VISUALIZE) {
            throw new DraftNotSaveableException(Reason.NOT_READY, "This job isn't a visualization draft.");
        }
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new DraftNotSaveableException(Reason.NOT_READY, "This draft isn't ready yet.");
        }

        JsonNode input = jobJson.read(job.getInputPayload());
        if (input.hasNonNull("conceptId")) {
            throw new DraftNotSaveableException(Reason.NOT_READY,
                    "This was a regenerate of an existing concept - it's already saved as a new version.");
        }
        if (job.getConceptId() != null) {
            return new DraftClaim(null, Optional.of(job.getConceptId()));
        }

        JsonNode result = jobJson.read(job.getResultPayload());
        if (!codec.isDraft(result)) {
            throw new DraftNotSaveableException(Reason.NOT_READY, "This job didn't produce a saveable draft.");
        }
        if (job.getUpdatedAt() != null && job.getUpdatedAt().plus(DRAFT_LIFETIME).isBefore(Instant.now())) {
            throw new DraftNotSaveableException(Reason.EXPIRED, "This draft has expired. Please generate it again.");
        }

        ConceptDraft draft = codec.readDraft(result, job.getId(), JobJson.optionalUuid(input, "sourceExplainJobId"));
        return new DraftClaim(draft, Optional.empty());
    }

    @Override
    public void markSaved(UUID draftId, UUID conceptId) {
        GenerationJob job = jobRepository.findById(draftId)
                .orElseThrow(() -> new NotFoundException("Draft not found: " + draftId));
        job.linkConcept(conceptId);
        jobRepository.save(job);
    }
}
