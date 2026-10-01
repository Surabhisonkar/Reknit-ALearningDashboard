package com.learningdashboard.backend.generation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptNote;
import com.learningdashboard.backend.concept.ConceptNoteService;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.generation.model.ChatMessage;
import com.learningdashboard.backend.generation.pipeline.ChatNotePipeline;
import com.learningdashboard.backend.generation.pipeline.ConceptChatPipeline;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatJobHandlersTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ChatJobCodec codec = new ChatJobCodec(new JobJson(mapper));
    private final ConceptService conceptService = mock(ConceptService.class);
    private final UUID userId = UUID.randomUUID();
    private final Concept concept = new Concept(userId, "Posture", "Stack ears over hips.", null, "animation", "{}", 1, null);
    private final List<ChatMessage> history = List.of(new ChatMessage("user", "Why?"), new ChatMessage("assistant", "Because."));

    private GenerationJob job(JobType type, String question) {
        return new GenerationJob(userId, type, codec.toInputJson(concept.getId(), history, question));
    }

    @Test
    void askAnswersFromTheConceptAndHistoryAndWritesNothing() throws Exception {
        ConceptChatPipeline pipeline = mock(ConceptChatPipeline.class);
        when(conceptService.requireOwnedConcept(concept.getId(), userId)).thenReturn(concept);
        when(pipeline.run(any(), eq(history), eq("How?"))).thenReturn(new ConceptChatPipeline.Result("Like this.", "raw"));

        JobResult result = new AskConceptJobHandler(conceptService, pipeline, codec).handle(job(JobType.ASK_CONCEPT, "How?"));

        JsonNode payload = mapper.readTree(result.resultPayloadJson());
        assertThat(payload.path("kind").asText()).isEqualTo("ANSWER");
        assertThat(payload.path("answer").asText()).isEqualTo("Like this.");
        assertThat(result.conceptId()).isNull();
    }

    @Test
    void chatToNoteSavesTheCondensedNoteAndReturnsIt() throws Exception {
        ChatNotePipeline pipeline = mock(ChatNotePipeline.class);
        ConceptNoteService notes = mock(ConceptNoteService.class);
        ConceptNote saved = new ConceptNote(concept.getId(), userId, "Condensed.", "chat");
        when(conceptService.requireOwnedConcept(concept.getId(), userId)).thenReturn(concept);
        when(pipeline.run(any(), eq(history))).thenReturn(new ChatNotePipeline.Result("Condensed.", "raw"));
        when(notes.addNote(concept.getId(), userId, "Condensed.", "chat")).thenReturn(saved);

        JobResult result = new ChatToNoteJobHandler(conceptService, notes, pipeline, codec).handle(job(JobType.CHAT_TO_NOTE, null));

        JsonNode payload = mapper.readTree(result.resultPayloadJson());
        assertThat(payload.path("kind").asText()).isEqualTo("NOTE");
        assertThat(payload.path("noteId").asText()).isEqualTo(saved.getId().toString());
        assertThat(payload.path("content").asText()).isEqualTo("Condensed.");
    }

    @Test
    void aConceptDeletedMidChatFailsBeforeAnyAiCall() {
        ChatNotePipeline pipeline = mock(ChatNotePipeline.class);
        when(conceptService.requireOwnedConcept(concept.getId(), userId)).thenThrow(new NotFoundException("Concept not found."));

        ChatToNoteJobHandler handler = new ChatToNoteJobHandler(conceptService, mock(ConceptNoteService.class), pipeline, codec);

        assertThatThrownBy(() -> handler.handle(job(JobType.CHAT_TO_NOTE, null))).isInstanceOf(NotFoundException.class);
        verify(pipeline, never()).run(any(), any());
    }

    @Test
    void theCodecRoundTripsTheChatInput() {
        ChatJobCodec.ChatInput input = codec.readInput(codec.toInputJson(concept.getId(), history, "Q?"));

        assertThat(input.conceptId()).isEqualTo(concept.getId());
        assertThat(input.history()).isEqualTo(history);
        assertThat(input.question()).isEqualTo("Q?");
        assertThat(codec.readInput(codec.toInputJson(concept.getId(), history, null)).question()).isNull();
    }
}
