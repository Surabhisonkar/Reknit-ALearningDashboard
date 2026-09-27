package com.learningdashboard.backend.rag;

import com.learningdashboard.backend.concept.ConceptIndexer;
import com.learningdashboard.backend.generation.job.GenerationJobService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter for the concept module's {@link ConceptIndexer} port: enqueues
 * an {@code INDEX_CONCEPT} job so the embedding-provider call happens in
 * the worker (the api service never calls an AI provider itself).
 *
 * <p>The job is enqueued <em>after</em> the current transaction commits:
 * enqueueing earlier would let the worker pick it up before the concept
 * row is visible, and a rolled-back save must not leave an index job
 * behind. A failure to enqueue is logged, never propagated - the concept
 * is already saved, and a missing embedding only means it won't show up
 * as RAG context until its next regenerate.
 */
@Component
public class QueuedConceptIndexer implements ConceptIndexer {

    private static final Logger log = LoggerFactory.getLogger(QueuedConceptIndexer.class);

    private final GenerationJobService jobService;

    public QueuedConceptIndexer(GenerationJobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void requestIndexing(UUID conceptId, UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue(conceptId, userId);
                }
            });
        } else {
            enqueue(conceptId, userId);
        }
    }

    private void enqueue(UUID conceptId, UUID userId) {
        try {
            jobService.submitIndexJob(userId, conceptId);
        } catch (RuntimeException e) {
            log.error("Couldn't enqueue embedding index for concept {} - it won't be RAG-retrievable until re-indexed.",
                    conceptId, e);
        }
    }
}
