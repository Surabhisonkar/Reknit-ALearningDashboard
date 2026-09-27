package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.learningdashboard.backend.generation.pipeline.ExplainPipeline;
import org.springframework.stereotype.Component;

/** EXPLAIN jobs - moved verbatim from the former {@code JobProcessingService.processExplain}. */
@Component
public class ExplainJobHandler implements JobHandler {

    private final ExplainPipeline explainPipeline;
    private final JobJson jobJson;

    public ExplainJobHandler(ExplainPipeline explainPipeline, JobJson jobJson) {
        this.explainPipeline = explainPipeline;
        this.jobJson = jobJson;
    }

    @Override
    public JobType type() {
        return JobType.EXPLAIN;
    }

    @Override
    public JobResult handle(GenerationJob job) {
        JsonNode input = jobJson.read(job.getInputPayload());
        String topic = input.path("topic").asText();
        String userNotes = input.hasNonNull("userNotes") ? input.path("userNotes").asText() : null;

        ExplainPipeline.Result result = explainPipeline.run(topic, userNotes);

        return new JobResult(result.rawModelResponse(), jobJson.write(result.explanation()), null);
    }
}
