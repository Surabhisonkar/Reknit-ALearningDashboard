package com.learningdashboard.backend.generation.pipeline;

import com.learningdashboard.backend.generation.model.ChatConceptContext;
import com.learningdashboard.backend.generation.model.ChatMessage;
import com.learningdashboard.backend.generation.prompt.ConceptChatPromptBuilder;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import java.util.List;
import org.springframework.stereotype.Component;

/** One "Ask the AI" turn: exactly one provider call, a short conversational answer. */
@Component
public class ConceptChatPipeline {

    public static final int MAX_ANSWER_CHARS = 2000;

    private final TextGenerationProvider textProvider;
    private final ChatHistoryPolicy historyPolicy;
    private final ChatReplyParser replyParser;

    public ConceptChatPipeline(TextGenerationProvider textProvider, ChatHistoryPolicy historyPolicy,
                               ChatReplyParser replyParser) {
        this.textProvider = textProvider;
        this.historyPolicy = historyPolicy;
        this.replyParser = replyParser;
    }

    public Result run(ChatConceptContext concept, List<ChatMessage> history, String question) {
        ConceptChatPromptBuilder prompt = new ConceptChatPromptBuilder()
                .withConcept(concept)
                .withHistory(historyPolicy.trim(history))
                .withQuestion(question);
        String raw = textProvider.generateText(prompt.buildSystemPrompt(), prompt.buildUserPrompt());
        return new Result(replyParser.readField(raw, "answer", MAX_ANSWER_CHARS), raw);
    }

    public record Result(String answer, String rawModelResponse) { }
}
