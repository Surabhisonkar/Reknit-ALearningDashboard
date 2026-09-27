package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs only in the worker ECS service. This - not any controller - is
 * the one place in the codebase that actually invokes an AI provider.
 * Loads the job row (never trusts the SQS message body beyond "here's
 * an id"), routes by {@link JobType} to the matching {@link JobHandler},
 * and records the outcome, including the raw model response for audit
 * alongside the normalized result.
 *
 * <p>Handlers are discovered from the Spring context and registered by
 * type, so a new job type needs no change here (Open/Closed).
 */
@Service
public class JobProcessingService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    private final GenerationJobRepository jobRepository;
    private final Map<JobType, JobHandler> handlers;

    public JobProcessingService(GenerationJobRepository jobRepository, List<JobHandler> jobHandlers) {
        this.jobRepository = jobRepository;
        this.handlers = indexByType(jobHandlers);
    }

    private static Map<JobType, JobHandler> indexByType(List<JobHandler> jobHandlers) {
        Map<JobType, JobHandler> byType = new EnumMap<>(JobType.class);
        for (JobHandler handler : jobHandlers) {
            JobHandler previous = byType.put(handler.type(), handler);
            if (previous != null) {
                throw new IllegalStateException("Two JobHandlers registered for " + handler.type() + ": "
                        + previous.getClass().getSimpleName() + " and " + handler.getClass().getSimpleName());
            }
        }
        for (JobType type : JobType.values()) {
            if (!byType.containsKey(type)) {
                log.warn("No JobHandler registered for job type {} - such jobs will fail.", type);
            }
        }
        return Collections.unmodifiableMap(byType);
    }

    @Transactional
    public void process(UUID jobId) {
        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job {} no longer exists, skipping (already processed/deleted).", jobId);
            return;
        }
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            log.info("Job {} already in terminal state {}, skipping (idempotent redelivery).", jobId, job.getStatus());
            return;
        }

        job.markProcessing();
        jobRepository.save(job);

        try {
            JobHandler handler = handlers.get(job.getJobType());
            if (handler == null) {
                throw new IllegalStateException("No JobHandler for job type " + job.getJobType());
            }
            JobResult result = handler.handle(job);
            job.markCompleted(result.rawModelResponse(), result.resultPayloadJson(), result.conceptId());
            jobRepository.save(job);
        } catch (GenerationException e) {
            log.error("Job {} failed [{}]: {}", jobId, e.getCode(), e.getDetails() != null ? e.getDetails() : e.getMessage());
            job.markFailed(safeMessage(e), null);
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Job {} failed with an unexpected error.", jobId, e);
            job.markFailed("Something went wrong generating this. Please try again.", null);
            jobRepository.save(job);
        }
    }

    /** Never store/leak provider internals in the user-facing error message. */
    private String safeMessage(GenerationException e) {
        return switch (e.getCode()) {
            case INVALID_JSON, SCHEMA_VALIDATION_FAILED ->
                    "The AI couldn't generate a usable result. Please try rephrasing.";
            case ALL_PROVIDERS_FAILED ->
                    "All configured AI providers are currently unavailable. Please try again shortly.";
            case CONTENT_SAFETY_REJECTED ->
                    "The generated content didn't pass our content-safety checks. Please try rephrasing.";
        };
    }
}
