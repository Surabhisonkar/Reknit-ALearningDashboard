package com.learningdashboard.backend.generation.provider.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.EmbeddingProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final String INSTANCE_NAME = "gemini-embedding";

    private final RestClient restClient;
    private final ResilientProviderExecutor executor;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getGeminiApiKey())) {
            throw new IllegalStateException("Gemini embedding provider requires GEMINI_API_KEY to be set.");
        }
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(ClientHttpRequestFactorySettings.defaults()
                        .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))))
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/"
                        + aiProperties.getGeminiEmbeddingModel() + ":embedContent")
                .defaultHeader("content-type", "application/json")
                .defaultHeader("x-goog-api-key", aiProperties.getGeminiApiKey())
                .build();
    }

    @Override
    public float[] embed(String text) {
        return executor.execute(INSTANCE_NAME, () -> doEmbed(text));
    }

    @Override
    public String name() {
        return INSTANCE_NAME;
    }

    private float[] doEmbed(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("content").putArray("parts").addObject().put("text", text);

        JsonNode response = restClient.post().body(body).retrieve().body(JsonNode.class);
        JsonNode values = response == null ? null : response.path("embedding").path("values");
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalStateException("Gemini did not return an embedding for this text.");
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
