package com.learningdashboard.backend.generation.provider.openai;

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

/**
 * Talks to OpenAI's Chat Completions API. Included as a third fallback
 * link for text generation — proof that the interface genuinely supports
 * adding a provider without touching {@link
 * com.learningdashboard.backend.generation.provider.ProviderFactory}'s
 * callers: this class, one new @Bean method, and one config value
 * ({@code TEXT_PROVIDER_ORDER=claude,gemini,openai}) is the entire diff.
 */
public class OpenAiTextProvider implements TextGenerationProvider {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String INSTANCE_NAME = "openai-text";

    private final RestClient restClient;
    private final ResilientProviderExecutor executor;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiTextProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getOpenaiApiKey())) {
            throw new IllegalStateException("OpenAI provider requires OPENAI_API_KEY to be set.");
        }
        this.aiProperties = aiProperties;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(ClientHttpRequestFactorySettings.defaults()
                        .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))))
                .baseUrl(ENDPOINT)
                .defaultHeader("Authorization", "Bearer " + aiProperties.getOpenaiApiKey())
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
        body.put("model", aiProperties.getOpenaiTextModel());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        JsonNode response = restClient.post().body(body).retrieve().body(JsonNode.class);
        JsonNode content = response == null ? null
                : response.path("choices").path(0).path("message").path("content");

        if (content == null || !content.isTextual()) {
            throw new IllegalStateException("OpenAI response contained no text content.");
        }
        return content.asText();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
