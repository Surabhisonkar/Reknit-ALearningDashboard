package com.learningdashboard.backend.generation.provider.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/** Talks to the Anthropic Messages API. Good at: structured reasoning over text (explanations, structuring visualizations). */
public class ClaudeTextProvider implements TextGenerationProvider {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String INSTANCE_NAME = "claude-text";

    private final RestClient restClient;
    private final ResilientProviderExecutor executor;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public ClaudeTextProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getAnthropicApiKey())) {
            throw new IllegalStateException("Claude provider requires ANTHROPIC_API_KEY to be set.");
        }
        this.aiProperties = aiProperties;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(ClientHttpRequestFactorySettings.defaults()
                        .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))))
                .baseUrl(ENDPOINT)
                .defaultHeader("x-api-key", aiProperties.getAnthropicApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("content-type", "application/json")
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
        body.put("model", aiProperties.getClaudeModel());
        body.put("max_tokens", 1500);
        body.put("system", systemPrompt);

        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", userPrompt);

        JsonNode response = restClient.post().body(body).retrieve().body(JsonNode.class);
        if (response == null || !response.has("content")) {
            throw new IllegalStateException("Claude response contained no content.");
        }
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText();
            }
        }
        throw new IllegalStateException("Claude response contained no text content.");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
