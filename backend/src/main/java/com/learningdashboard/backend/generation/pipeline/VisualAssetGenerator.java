package com.learningdashboard.backend.generation.pipeline;

import com.learningdashboard.backend.generation.model.AnimationPayload;
import com.learningdashboard.backend.generation.model.ImagePayload;
import com.learningdashboard.backend.generation.model.VisualizationPayload;
import com.learningdashboard.backend.generation.provider.VisualGenerationProvider;
import com.learningdashboard.backend.storage.Artifact;
import com.learningdashboard.backend.storage.ArtifactStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates the image/animation-scene assets a payload asks for, uploads
 * them via {@link ArtifactStorage#uploadGenerated} - deliberately
 * <em>not</em> attached to any concept, so each one starts life with the
 * existing orphan-expiry TTL - and returns the payload rebuilt with the
 * new artifact ids filled in. Attaching happens later, only if the draft
 * is actually saved (see {@code ConceptVersionWriter}).
 *
 * <p>Extracted unchanged from {@code VisualizePipeline}: same provider
 * calls, same payload rebuilding, minus the attach step.
 */
@Component
public class VisualAssetGenerator {

    private final VisualGenerationProvider visualProvider;
    private final ArtifactStorage artifactStorage;

    public VisualAssetGenerator(VisualGenerationProvider visualProvider, ArtifactStorage artifactStorage) {
        this.visualProvider = visualProvider;
        this.artifactStorage = artifactStorage;
    }

    public Result generate(VisualizationPayload payload, UUID userId, UUID generationJobId) {
        List<UUID> artifactIds = new ArrayList<>();

        if (payload instanceof ImagePayload image) {
            Artifact artifact = generateAndUpload(image.imagePrompt(), userId, generationJobId);
            artifactIds.add(artifact.getId());
            return new Result(
                    new ImagePayload(image.version(), image.imagePrompt(), artifact.getId().toString(), image.altText()),
                    artifactIds);
        }

        if (payload instanceof AnimationPayload animation) {
            List<AnimationPayload.Scene> updatedScenes = new ArrayList<>();
            for (AnimationPayload.Scene scene : animation.scenes()) {
                if (scene.needsVisualAsset() && scene.visualPrompt() != null && !scene.visualPrompt().isBlank()) {
                    Artifact artifact = generateAndUpload(scene.visualPrompt(), userId, generationJobId);
                    artifactIds.add(artifact.getId());
                    updatedScenes.add(new AnimationPayload.Scene(
                            scene.id(), scene.order(), scene.title(), scene.narration(),
                            scene.durationSeconds(), scene.transitionToNext(), scene.needsVisualAsset(),
                            scene.visualPrompt(), List.of(artifact.getId().toString())));
                } else {
                    updatedScenes.add(scene);
                }
            }
            return new Result(new AnimationPayload(animation.version(), updatedScenes), artifactIds);
        }

        // mind_map and diagram carry no generated visual assets today
        return new Result(payload, artifactIds);
    }

    private Artifact generateAndUpload(String prompt, UUID userId, UUID generationJobId) {
        var asset = visualProvider.generateVisual(prompt);
        return artifactStorage.uploadGenerated(asset.bytes(), asset.mimeType(), userId, generationJobId);
    }

    /** The payload with artifact ids filled in, plus those ids as a flat list (what gets attached on save). */
    public record Result(VisualizationPayload payload, List<UUID> artifactIds) {
        public Result {
            artifactIds = List.copyOf(artifactIds);
        }
    }
}
