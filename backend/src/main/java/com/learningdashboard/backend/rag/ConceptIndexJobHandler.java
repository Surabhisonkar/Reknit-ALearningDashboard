package com.learningdashboard.backend.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptRepository;
import com.learningdashboard.backend.generation.job.GenerationJob;
import com.learningdashboard.backend.generation.job.JobHandler;
import com.learningdashboard.backend.generation.job.JobJson;
import com.learningdashboard.backend.generation.job.JobResult;
import com.learningdashboard.backend.generation.job.JobType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Worker side of {@link QueuedConceptIndexer}: embeds the concept's
 * <em>current</em> title + summary (read at processing time, so a burst
 * of saves/regenerates always converges on the latest text) and upserts
 * it via {@link EmbeddingIndexService}. A concept deleted in the meantime
 * is simply skipped.
 */
@Component
public class ConceptIndexJobHandler implements JobHandler {

    private final ConceptRepository conceptRepository;
    private final EmbeddingIndexService embeddingIndexService;
    private final JobJson jobJson;

    public ConceptIndexJobHandler(ConceptRepository conceptRepository, EmbeddingIndexService embeddingIndexService,
                                  JobJson jobJson) {
        this.conceptRepository = conceptRepository;
        this.embeddingIndexService = embeddingIndexService;
        this.jobJson = jobJson;
    }

    @Override
    public JobType type() {
        return JobType.INDEX_CONCEPT;
    }

    @Override
    public JobResult handle(GenerationJob job) {
        JsonNode input = jobJson.read(job.getInputPayload());
        UUID conceptId = JobJson.optionalUuid(input, "conceptId");

        Optional<Concept> concept = conceptId == null
                ? Optional.empty()
                : conceptRepository.findByIdAndUserId(conceptId, job.getUserId());
        if (concept.isEmpty()) {
            return new JobResult(null, "{\"indexed\":false,\"reason\":\"concept_not_found\"}", null);
        }

        Concept c = concept.get();
        embeddingIndexService.indexConcept(c.getId(), c.getUserId(), c.getTitle(), c.getSummary());
        return new JobResult(null, "{\"indexed\":true}", c.getId());
    }
}
