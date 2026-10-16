package com.learningdashboard.backend.generation.provider.gemini;

import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class GeminiTextProvider implements TextGenerationProvider {

    private static final String INSTANCE_NAME = "gemini-text";

    private final RestClient restClient;
    private final ResilientProviderExecutor executor;
    private final ObjectMapper objectMapper;

    public GeminiTextProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getGeminiApiKey())) {
            throw new IllegalStateException("Gemini text provider requires GEMINI_API_KEY to be set.");
        }
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                        .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))))
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/"
                        + aiProperties.getGeminiTextModel() + ":generateContent")
                .defaultHeader("content-type", "application/json")
                .defaultHeader("x-goog-api-key", aiProperties.getGeminiApiKey())
                .build();
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return executor.execute(INSTANCE_NAME, () -> doGenerate(systemPrompt, userPrompt));
    }

    @Override
    public String name() {
        return INSTANCE_NAME;
    }

    private String doGenerate(String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt);
        ArrayNode contents = body.putArray("contents");
        contents.addObject().put("role", "user").putArray("parts").addObject().put("text", userPrompt);

        JsonNode response = restClient.post().body(body).retrieve().body(JsonNode.class);
        JsonNode textNode = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode == null || !textNode.isTextual()) {
            throw new IllegalStateException("Gemini response contained no text content.");
        }
        return textNode.asText();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
