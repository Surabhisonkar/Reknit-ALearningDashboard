package com.learningdashboard.backend.generation.job;

/**
 * Strategy for one {@link JobType}. {@link JobProcessingService} owns the
 * job lifecycle (load, idempotency, PROCESSING/COMPLETED/FAILED, error
 * sanitizing) and delegates the actual work here - so adding a job type
 * is "write one handler bean", never "edit a switch" (Open/Closed).
 *
 * <p>Implementations may throw {@link
 * com.learningdashboard.backend.common.exception.GenerationException};
 * anything thrown is recorded on the job row by the processing service.
 */
public interface JobHandler {

    /** The single job type this handler processes. Exactly one handler per type is allowed. */
    JobType type();

    JobResult handle(GenerationJob job);
}
