package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.generation.model.ChatConceptContext;
import com.learningdashboard.backend.generation.pipeline.ConceptChatPipeline;
import org.springframework.stereotype.Component;

/**
 * ASK_CONCEPT: answers one chat question about a saved concept. Nothing is
 * written to the concept. Ownership is checked through a non-transactional
 * read, so a concept deleted mid-chat fails the job cleanly (handoff §4.1).
 */
@Component
public class AskConceptJobHandler implements JobHandler {

    private final ConceptService conceptService;
    private final ConceptChatPipeline chatPipeline;
    private final ChatJobCodec codec;

    public AskConceptJobHandler(ConceptService conceptService, ConceptChatPipeline chatPipeline, ChatJobCodec codec) {
        this.conceptService = conceptService;
        this.chatPipeline = chatPipeline;
        this.codec = codec;
    }

    @Override
    public JobType type() {
        return JobType.ASK_CONCEPT;
    }

    @Override
    public JobResult handle(GenerationJob job) {
        ChatJobCodec.ChatInput input = codec.readInput(job.getInputPayload());
        Concept concept = conceptService.requireOwnedConcept(input.conceptId(), job.getUserId());
        ConceptChatPipeline.Result result = chatPipeline.run(ChatContexts.of(concept), input.history(), input.question());
        return new JobResult(result.rawModelResponse(), codec.answerResultJson(result.answer()), null);
    }
}
