package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptNote;
import com.learningdashboard.backend.concept.ConceptNoteService;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.generation.pipeline.ChatNotePipeline;
import org.springframework.stereotype.Component;

/**
 * CHAT_TO_NOTE: condenses a Spark chat and saves it as a note on the concept.
 * Ownership is checked (non-transactionally) before the AI call, so a deleted
 * concept fails the job cleanly and no provider quota is spent on it.
 */
@Component
public class ChatToNoteJobHandler implements JobHandler {

    private final ConceptService conceptService;
    private final ConceptNoteService noteService;
    private final ChatNotePipeline notePipeline;
    private final ChatJobCodec codec;

    public ChatToNoteJobHandler(ConceptService conceptService, ConceptNoteService noteService,
                                ChatNotePipeline notePipeline, ChatJobCodec codec) {
        this.conceptService = conceptService;
        this.noteService = noteService;
        this.notePipeline = notePipeline;
        this.codec = codec;
    }

    @Override
    public JobType type() {
        return JobType.CHAT_TO_NOTE;
    }

    @Override
    public JobResult handle(GenerationJob job) {
        ChatJobCodec.ChatInput input = codec.readInput(job.getInputPayload());
        Concept concept = conceptService.requireOwnedConcept(input.conceptId(), job.getUserId());
        ChatNotePipeline.Result result = notePipeline.run(ChatContexts.of(concept), input.history());
        ConceptNote note = noteService.addNote(concept.getId(), job.getUserId(), result.note(), ConceptNote.SOURCE_CHAT);
        return new JobResult(result.rawModelResponse(), codec.noteResultJson(note.getId(), note.getContent()), null);
    }
}
