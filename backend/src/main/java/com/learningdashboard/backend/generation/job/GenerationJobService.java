package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only thing an HTTP controller talks to for job creation. Creates
 * the row, enqueues it, returns immediately — this class never calls an
 * AI provider itself, that only happens in the worker service via
 * {@link JobProcessingService}.
 */
@Service
public class GenerationJobService {

    private final GenerationJobRepository jobRepository;
    private final JobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public GenerationJobService(GenerationJobRepository jobRepository, JobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationJob submitExplainJob(UUID userId, String topic, String userNotes) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("topic", topic);
        if (userNotes != null) {
            input.put("userNotes", userNotes);
        }
        return submit(userId, JobType.EXPLAIN, input);
    }

    @Transactional
    public GenerationJob submitVisualizeJob(UUID userId, String conceptText, String preferredType,
                                             UUID sourceExplainJobId, UUID conceptId) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("conceptText", conceptText);
        input.put("preferredType", preferredType == null ? "auto" : preferredType);
        if (sourceExplainJobId != null) {
            input.put("sourceExplainJobId", sourceExplainJobId.toString());
        }
        // Present only for a regenerate - see VisualizeJobHandler (draft vs. append-new-version).
        if (conceptId != null) {
            input.put("conceptId", conceptId.toString());
        }
        return submit(userId, JobType.VISUALIZE, input);
    }

    /**
     * Post-save regenerate of an existing concept. Any field the caller
     * leaves empty is taken from the most recent Visualize job that
     * produced this concept (its original text, preferred type and
     * Explain link), so "Regenerate" means "same input, try again". If no
     * such job exists (e.g. very old data), {@code fallbackConceptText}
     * - built by the caller from the concept's title and summary - is used.
     * Caller must already have checked that the concept is the user's.
     */
    @Transactional
    public GenerationJob submitRegenerateJob(UUID userId, UUID conceptId, String conceptTextOverride,
                                             String preferredTypeOverride, String fallbackConceptText) {
        JsonNode original = jobRepository
                .findFirstByUserIdAndConceptIdAndJobTypeOrderByCreatedAtDesc(userId, conceptId, JobType.VISUALIZE)
                .map(job -> readJson(job.getInputPayload()))
                .orElse(objectMapper.createObjectNode());

        String conceptText = firstNonBlank(conceptTextOverride, original.path("conceptText").asText(null), fallbackConceptText);
        String preferredType = firstNonBlank(preferredTypeOverride, original.path("preferredType").asText(null), "auto");
        UUID sourceExplainJobId = original.hasNonNull("sourceExplainJobId")
                ? UUID.fromString(original.path("sourceExplainJobId").asText())
                : null;

        return submitVisualizeJob(userId, conceptText, preferredType, sourceExplainJobId, conceptId);
    }

    /**
     * Enqueues an embedding (re-)index of a saved concept. Runs in its own
     * transaction because it's normally called from an after-commit hook
     * (see {@code QueuedConceptIndexer}), where the outer transaction has
     * already finished.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenerationJob submitIndexJob(UUID userId, UUID conceptId) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("conceptId", conceptId.toString());
        return submit(userId, JobType.INDEX_CONCEPT, input);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private GenerationJob submit(UUID userId, JobType type, ObjectNode input) {
        GenerationJob job = new GenerationJob(userId, type, input.toString());
        job = jobRepository.save(job);
        jobQueue.enqueue(job.getId());
        return job;
    }

    /** Ownership-checked lookup for the GET /api/jobs/{id} status endpoint. */
    public GenerationJob requireOwnedJob(UUID jobId, UUID userId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));
    }
}
