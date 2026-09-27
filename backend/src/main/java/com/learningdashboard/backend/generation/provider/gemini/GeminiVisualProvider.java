package com.learningdashboard.backend.generation.provider.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.VisualGenerationProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/** Talks to Gemini's generateContent endpoint for image generation. Good at: concrete visual/photographic subjects. */
public class GeminiVisualProvider implements VisualGenerationProvider {

    private static final String INSTANCE_NAME = "gemini-visual";

    private final RestClient restClient;
    private final ResilientProviderExecutor executor;
    private final ObjectMapper objectMapper;

    public GeminiVisualProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getGeminiApiKey())) {
            throw new IllegalStateException("Gemini visual provider requires GEMINI_API_KEY to be set.");
        }
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(ClientHttpRequestFactorySettings.defaults()
                        .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))))
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/"
                        + aiProperties.getGeminiVisualModel() + ":generateContent")
                .defaultHeader("content-type", "application/json")
                .defaultHeader("x-goog-api-key", aiProperties.getGeminiApiKey())
                .build();
    }

    @Override
    public VisualAsset generateVisual(String prompt) {
        return executor.execute(INSTANCE_NAME, () -> doGenerate(prompt));
    }

    @Override
    public String name() {
        return INSTANCE_NAME;
    }

    private VisualAsset doGenerate(String prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        contents.addObject().putArray("parts").addObject().put("text", prompt);
        ArrayNode modalities = body.putObject("generationConfig").putArray("responseModalities");
        modalities.add("TEXT");
        modalities.add("IMAGE");

        JsonNode response = restClient.post().body(body).retrieve().body(JsonNode.class);
        JsonNode parts = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts");

        if (parts != null && parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode inlineData = part.path("inlineData");
                if (inlineData.has("data")) {
                    String mimeType = inlineData.path("mimeType").asText("image/png");
                    byte[] bytes = Base64.getDecoder().decode(inlineData.path("data").asText());
                    return new VisualAsset(bytes, mimeType);
                }
            }
        }
        throw new IllegalStateException("Gemini did not return an image for this prompt.");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
