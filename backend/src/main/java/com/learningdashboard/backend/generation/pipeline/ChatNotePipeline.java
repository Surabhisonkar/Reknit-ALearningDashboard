package com.learningdashboard.backend.generation.pipeline;

import com.learningdashboard.backend.generation.model.ChatConceptContext;
import com.learningdashboard.backend.generation.model.ChatMessage;
import com.learningdashboard.backend.generation.prompt.ChatNotePromptBuilder;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import java.util.List;
import org.springframework.stereotype.Component;

/** Condenses a Spark chat into a note of a few lines: exactly one provider call. Saving it is the job handler's job. */
@Component
public class ChatNotePipeline {

    public static final int MAX_NOTE_CHARS = 1000;

    private final TextGenerationProvider textProvider;
    private final ChatHistoryPolicy historyPolicy;
    private final ChatReplyParser replyParser;

    public ChatNotePipeline(TextGenerationProvider textProvider, ChatHistoryPolicy historyPolicy,
                            ChatReplyParser replyParser) {
        this.textProvider = textProvider;
        this.historyPolicy = historyPolicy;
        this.replyParser = replyParser;
    }

    public Result run(ChatConceptContext concept, List<ChatMessage> history) {
        ChatNotePromptBuilder prompt = new ChatNotePromptBuilder()
                .withConcept(concept)
                .withHistory(historyPolicy.trim(history));
        String raw = textProvider.generateText(prompt.buildSystemPrompt(), prompt.buildUserPrompt());
        return new Result(replyParser.readField(raw, "note", MAX_NOTE_CHARS), raw);
    }

    public record Result(String note, String rawModelResponse) { }
}
