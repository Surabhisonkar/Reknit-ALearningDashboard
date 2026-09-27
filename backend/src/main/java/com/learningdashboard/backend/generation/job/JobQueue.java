package com.learningdashboard.backend.generation.job;

import java.util.List;
import java.util.UUID;

/**
 * The api service only ever calls {@link #enqueue}; the worker service
 * only ever calls {@link #poll} and {@link #acknowledge}/{@link #release}.
 * Neither side knows or cares that SQS is behind this — swapping the
 * broker (e.g. to Kafka/RabbitMQ later) is a new implementation of this
 * interface, zero changes to {@code GenerationJobService} or {@code
 * GenerationJobWorker}.
 */
public interface JobQueue {

    void enqueue(UUID jobId);

    /** Long-polls for up to N job ids to process. Returns an empty list if none are available. */
    List<QueuedJob> poll();

    /** Call after a job has been fully processed (success or terminal failure) so it isn't redelivered. */
    void acknowledge(QueuedJob queuedJob);

    /** Call to put a job back for retry (e.g. a transient failure) instead of acknowledging it. */
    void release(QueuedJob queuedJob);

    record QueuedJob(UUID jobId, String receiptHandle) { }
}
