package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.generation.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The only class that knows the chat jobs' JSON: the input stored on
 * ASK_CONCEPT / CHAT_TO_NOTE jobs, and their results
 * ({"kind":"ANSWER","answer"} and {"kind":"NOTE","noteId","content"}, which
 * the app reads when it polls the job). Written by GenerationJobService,
 * read by the two handlers, so both sides can't drift apart.
 */
@Component
public class ChatJobCodec {

    private final JobJson jobJson;

    public ChatJobCodec(JobJson jobJson) {
        this.jobJson = jobJson;
    }

    public String toInputJson(UUID conceptId, List<ChatMessage> history, String question) {
        ObjectNode input = jobJson.mapper().createObjectNode();
        input.put("conceptId", conceptId.toString());
        ArrayNode messages = input.putArray("history");
        for (ChatMessage message : history == null ? List.<ChatMessage>of() : history) {
            messages.addObject().put("role", message.role()).put("text", message.text());
        }
        if (question != null) {
            input.put("question", question);
        }
        return input.toString();
    }

    public ChatInput readInput(String inputJson) {
        JsonNode input = jobJson.read(inputJson);
        List<ChatMessage> history = new ArrayList<>();
        for (JsonNode message : input.path("history")) {
            history.add(new ChatMessage(message.path("role").asText(), message.path("text").asText()));
        }
        String question = input.hasNonNull("question") ? input.path("question").asText() : null;
        return new ChatInput(UUID.fromString(input.path("conceptId").asText()), history, question);
    }

    public String answerResultJson(String answer) {
        ObjectNode result = jobJson.mapper().createObjectNode();
        result.put("kind", "ANSWER");
        result.put("answer", answer);
        return result.toString();
    }

    public String noteResultJson(UUID noteId, String content) {
        ObjectNode result = jobJson.mapper().createObjectNode();
        result.put("kind", "NOTE");
        result.put("noteId", noteId.toString());
        result.put("content", content);
        return result.toString();
    }

    public record ChatInput(UUID conceptId, List<ChatMessage> history, String question) { }
}
