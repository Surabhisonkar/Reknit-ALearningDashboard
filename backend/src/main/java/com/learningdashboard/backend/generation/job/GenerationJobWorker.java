package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.config.WorkerProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The only place that long-polls SQS. Runs on its own single background
 * thread (SQS long-polling with {@code waitTimeSeconds} already blocks
 * efficiently, so one thread per worker task is enough — ECS scales
 * throughput by adding more worker tasks, not more threads per task).
 *
 * <p>{@code @Profile("worker")} means this component — and therefore any
 * actual LLM invocation — simply doesn't exist in the api service's
 * application context. A bug in the polling loop can't affect the
 * public-facing service, and the two scale independently in ECS.
 */
@Component
@Profile("worker")
public class GenerationJobWorker {

    private static final Logger log = LoggerFactory.getLogger(GenerationJobWorker.class);

    private final JobQueue jobQueue;
    private final JobProcessingService jobProcessingService;
    private final WorkerProperties workerProperties;
    private final ExecutorService pollingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "generation-job-poller");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);

    public GenerationJobWorker(JobQueue jobQueue, JobProcessingService jobProcessingService, WorkerProperties workerProperties) {
        this.jobQueue = jobQueue;
        this.jobProcessingService = jobProcessingService;
        this.workerProperties = workerProperties;
    }

    @PostConstruct
    void start() {
        if (!workerProperties.isPollEnabled()) {
            log.info("Worker polling disabled (WORKER_POLL_ENABLED=false).");
            return;
        }
        running.set(true);
        pollingExecutor.submit(this::pollLoop);
        log.info("Generation job worker started, polling SQS.");
    }

    @PreDestroy
    void stop() {
        running.set(false);
        pollingExecutor.shutdownNow();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                List<JobQueue.QueuedJob> jobs = jobQueue.poll();
                for (JobQueue.QueuedJob queuedJob : jobs) {
                    processOne(queuedJob);
                }
            } catch (Exception e) {
                // Never let the loop die on a transient SQS/DB blip - log and
                // keep polling. A sustained outage is visible via CloudWatch
                // metrics/alarms on this log pattern, not a crashed task.
                log.error("Error in job polling loop, will retry.", e);
                sleepBackoff();
            }
        }
    }

    private void processOne(JobQueue.QueuedJob queuedJob) {
        try {
            jobProcessingService.process(queuedJob.jobId());
            jobQueue.acknowledge(queuedJob);
        } catch (Exception e) {
            // JobProcessingService already catches and records failures on the
            // job row itself; reaching here means something failed outside
            // that (e.g. a DB outage mid-save) - release for SQS redelivery
            // rather than acknowledging a job we couldn't actually record.
            log.error("Unhandled error processing job {}, releasing for redelivery.", queuedJob.jobId(), e);
            jobQueue.release(queuedJob);
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
