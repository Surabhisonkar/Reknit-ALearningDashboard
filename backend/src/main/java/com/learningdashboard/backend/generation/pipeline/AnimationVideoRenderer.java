package com.learningdashboard.backend.generation.pipeline;

import com.learningdashboard.backend.config.AnimationRendererProperties;
import com.learningdashboard.backend.generation.model.AnimationPayload;
import com.learningdashboard.backend.storage.ArtifactRepository;
import com.learningdashboard.backend.storage.ArtifactStorage;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Turns a validated animation storyboard into an MP4 by calling the
 * Remotion render service ({@code POST {rendererUrl}/render}), then stores
 * the video in S3 through {@link ArtifactStorage} like any other generated
 * asset - orphaned until the draft is saved.
 *
 * <p>The video is a convenience, not the content: the storyboard itself is
 * what's saved, and the frontend plays it with the same Remotion
 * composition when there's no MP4. So a missing or failing renderer is
 * logged and skipped, never a failed job.
 */
@Component
public class AnimationVideoRenderer {

    private static final Logger log = LoggerFactory.getLogger(AnimationVideoRenderer.class);

    private final AnimationRendererProperties properties;
    private final ArtifactStorage artifactStorage;
    private final ArtifactRepository artifactRepository;
    private final RestClient restClient;

    public AnimationVideoRenderer(AnimationRendererProperties properties, ArtifactStorage artifactStorage,
                                  ArtifactRepository artifactRepository) {
        this.properties = properties;
        this.artifactStorage = artifactStorage;
        this.artifactRepository = artifactRepository;
        this.restClient = properties.isEnabled()
                ? RestClient.builder()
                        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                                .withReadTimeout(Duration.ofMillis(properties.getRenderTimeoutMs()))))
                        .baseUrl(properties.getRendererUrl())
                        .build()
                : null;
    }

    /** @return the MP4's artifact id, or null when no renderer is configured or rendering failed. */
    public UUID render(AnimationPayload storyboard, UUID userId, UUID generationJobId) {
        if (restClient == null) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of("storyboard", storyboard, "assetUrls", sceneAssetUrls(storyboard));
            byte[] video = restClient.post().uri("/render").body(body).retrieve().body(byte[].class);
            if (video == null || video.length == 0) {
                log.warn("Animation renderer returned no video for job {}.", generationJobId);
                return null;
            }
            return artifactStorage.uploadGenerated(video, "video/mp4", userId, generationJobId).getId();
        } catch (Exception e) {
            log.warn("Animation render failed for job {} ({}); the storyboard will play live instead.",
                    generationJobId, e.getMessage());
            return null;
        }
    }

    /** The renderer has no S3 access of its own, so scene images go to it as short-lived presigned URLs. */
    private Map<String, String> sceneAssetUrls(AnimationPayload storyboard) {
        Map<String, String> urls = new LinkedHashMap<>();
        for (AnimationPayload.Scene scene : storyboard.scenes()) {
            if (scene.assetArtifactIds() == null) continue;
            for (String id : scene.assetArtifactIds()) {
                artifactRepository.findById(UUID.fromString(id))
                        .ifPresent(artifact -> urls.put(id, artifactStorage.presignDownloadUrl(artifact).toString()));
            }
        }
        return urls;
    }
}
