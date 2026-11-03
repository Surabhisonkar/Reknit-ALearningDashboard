package com.learningdashboard.backend.generation.provider.qwen;

import com.learningdashboard.backend.config.AiProperties;
import com.learningdashboard.backend.generation.provider.VisualGenerationProvider;
import com.learningdashboard.backend.generation.provider.resilience.ResilientProviderExecutor;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Qwen-Image through Alibaba Cloud Model Studio (DashScope). Good at:
 * clean, detailed illustrations - which is what "Illustration" asks for,
 * so it heads {@code VISUAL_PROVIDER_ORDER} by default.
 *
 * <p>DashScope's text-to-image API is asynchronous: submit a task, poll it
 * until it succeeds, then download the image from the (24-hour) URL it
 * returns. The bytes are what this provider hands back, so storage is the
 * same S3 path as every other provider's.
 */
public class QwenImageProvider implements VisualGenerationProvider {

    private static final String INSTANCE_NAME = "qwen-image";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration MAX_WAIT = Duration.ofMinutes(3);

    private final RestClient restClient;
    private final RestClient downloadClient;
    private final ResilientProviderExecutor executor;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String size;

    public QwenImageProvider(AiProperties aiProperties, ResilientProviderExecutor executor, ObjectMapper objectMapper) {
        if (isBlank(aiProperties.getDashscopeApiKey())) {
            throw new IllegalStateException("Qwen-Image provider requires DASHSCOPE_API_KEY to be set.");
        }
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.model = aiProperties.getQwenImageModel();
        this.size = aiProperties.getQwenImageSize();
        var requestFactory = ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                .withReadTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs())));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(aiProperties.getDashscopeBaseUrl())
                .defaultHeader("content-type", "application/json")
                .defaultHeader("authorization", "Bearer " + aiProperties.getDashscopeApiKey())
                .build();
        this.downloadClient = RestClient.builder().requestFactory(requestFactory).build();
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
        body.put("model", model);
        body.putObject("input").put("prompt", prompt);
        ObjectNode parameters = body.putObject("parameters");
        parameters.put("size", size);
        parameters.put("n", 1);
        // The prompt already says exactly what to draw (and to draw no text); don't let it be rewritten.
        parameters.put("prompt_extend", false);
        parameters.put("watermark", false);

        JsonNode submitted = restClient.post()
                .uri("/services/aigc/text2image/image-synthesis")
                .header("X-DashScope-Async", "enable")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        String taskId = submitted == null ? "" : submitted.path("output").path("task_id").asText("");
        if (taskId.isBlank()) {
            throw new IllegalStateException("Qwen-Image did not accept the task.");
        }

        String imageUrl = awaitImageUrl(taskId);
        ResponseEntity<byte[]> image = downloadClient.get().uri(imageUrl).retrieve().toEntity(byte[].class);
        if (image.getBody() == null || image.getBody().length == 0) {
            throw new IllegalStateException("Qwen-Image returned an empty image.");
        }
        String mimeType = image.getHeaders().getContentType() == null
                ? "image/png" : image.getHeaders().getContentType().toString();
        return new VisualAsset(image.getBody(), mimeType);
    }

    private String awaitImageUrl(String taskId) {
        Instant deadline = Instant.now().plus(MAX_WAIT);
        while (Instant.now().isBefore(deadline)) {
            JsonNode task = restClient.get().uri("/tasks/{id}", taskId).retrieve().body(JsonNode.class);
            JsonNode output = task == null ? objectMapper.createObjectNode() : task.path("output");
            String status = output.path("task_status").asText("");
            switch (status) {
                case "SUCCEEDED" -> {
                    String url = output.path("results").path(0).path("url").asText("");
                    if (url.isBlank()) {
                        throw new IllegalStateException("Qwen-Image finished without an image URL.");
                    }
                    return url;
                }
                case "FAILED", "CANCELED", "UNKNOWN" -> throw new IllegalStateException(
                        "Qwen-Image task " + status + ": " + output.path("message").asText(""));
                default -> sleep();
            }
        }
        throw new IllegalStateException("Qwen-Image task " + taskId + " did not finish in " + MAX_WAIT.toSeconds() + "s.");
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Qwen-Image.", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
