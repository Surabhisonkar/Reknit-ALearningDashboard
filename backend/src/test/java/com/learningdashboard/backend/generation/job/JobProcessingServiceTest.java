package com.learningdashboard.backend.generation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobProcessingServiceTest {

    private final GenerationJobRepository repository = mock(GenerationJobRepository.class);

    private static JobHandler handler(JobType type, JobResult result, RuntimeException failure) {
        return new JobHandler() {
            @Override public JobType type() { return type; }
            @Override public JobResult handle(GenerationJob job) {
                if (failure != null) throw failure;
                return result;
            }
        };
    }

    private GenerationJob pendingJob(JobType type) {
        GenerationJob job = new GenerationJob(UUID.randomUUID(), type, "{}");
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    @Test
    void routesToTheHandlerForTheJobsType() {
        UUID conceptId = UUID.randomUUID();
        var service = new JobProcessingService(repository, List.of(
                handler(JobType.EXPLAIN, new JobResult("raw-e", "{\"e\":1}", null), null),
                handler(JobType.VISUALIZE, new JobResult("raw-v", "{\"v\":1}", conceptId), null)));
        GenerationJob job = pendingJob(JobType.VISUALIZE);

        service.process(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getResultPayload()).isEqualTo("{\"v\":1}");
        assertThat(job.getConceptId()).isEqualTo(conceptId);
        assertThat(job.getRawModelResponse()).isEqualTo("raw-v");
    }

    @Test
    void recordsAGenerationFailureWithASafeMessage() {
        var service = new JobProcessingService(repository, List.of(handler(JobType.EXPLAIN, null,
                new GenerationException("provider said x", GenerationException.Code.ALL_PROVIDERS_FAILED))));
        GenerationJob job = pendingJob(JobType.EXPLAIN);

        service.process(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).doesNotContain("provider said x");
    }

    @Test
    void failsAJobWithNoRegisteredHandler() {
        var service = new JobProcessingService(repository, List.of());
        GenerationJob job = pendingJob(JobType.INDEX_CONCEPT);

        service.process(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void refusesTwoHandlersForOneType() {
        assertThatThrownBy(() -> new JobProcessingService(repository, List.of(
                handler(JobType.EXPLAIN, null, null), handler(JobType.EXPLAIN, null, null))))
                .isInstanceOf(IllegalStateException.class);
    }
}
