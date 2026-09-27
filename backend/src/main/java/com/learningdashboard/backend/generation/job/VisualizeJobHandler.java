package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.concept.ConceptRegenerationService;
import com.learningdashboard.backend.concept.VersionAppended;
import com.learningdashboard.backend.generation.pipeline.VisualizePipeline;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * VISUALIZE jobs. Runs the (persistence-free) pipeline, then picks one of
 * two outcomes:
 * <ul>
 *   <li><b>no {@code conceptId} in the input</b> - a new capture: the draft
 *   becomes the job's result, and nothing is written to {@code concepts}.
 *   The user reviews it and saves it explicitly via {@code POST /api/concepts}.</li>
 *   <li><b>{@code conceptId} present</b> - a post-save regenerate: the draft
 *   is appended as a new version of that concept right away (ownership
 *   was already checked synchronously in {@code VisualizeController}).</li>
 * </ul>
 */
@Component
public class VisualizeJobHandler implements JobHandler {

    private final VisualizePipeline pipeline;
    private final VisualizationDraftCodec codec;
    private final ConceptRegenerationService regenerationService;
    private final JobJson jobJson;

    public VisualizeJobHandler(VisualizePipeline pipeline, VisualizationDraftCodec codec,
                               ConceptRegenerationService regenerationService, JobJson jobJson) {
        this.pipeline = pipeline;
        this.codec = codec;
        this.regenerationService = regenerationService;
        this.jobJson = jobJson;
    }

    @Override
    public JobType type() {
        return JobType.VISUALIZE;
    }

    @Override
    public JobResult handle(GenerationJob job) {
        JsonNode input = jobJson.read(job.getInputPayload());
        String conceptText = input.path("conceptText").asText();
        String preferredType = input.path("preferredType").asText("auto");
        UUID sourceExplainJobId = JobJson.optionalUuid(input, "sourceExplainJobId");
        UUID conceptId = JobJson.optionalUuid(input, "conceptId");

        VisualizePipeline.Result result = pipeline.run(job.getUserId(), job.getId(), conceptText, preferredType);

        if (conceptId == null) {
            return new JobResult(result.rawModelResponse(), codec.toDraftResultJson(result.draft()), null);
        }

        VersionAppended appended = regenerationService.appendVersion(conceptId, job.getUserId(),
                codec.toConceptDraft(result.draft(), job.getId(), sourceExplainJobId));
        return new JobResult(result.rawModelResponse(), codec.toVersionResultJson(appended), appended.conceptId());
    }
}
