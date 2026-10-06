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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generates the image/animation-scene assets a payload asks for, uploads
 * them via {@link ArtifactStorage#uploadGenerated} - deliberately
 * <em>not</em> attached to any concept, so each one starts life with the
 * existing orphan-expiry TTL - and returns the payload rebuilt with the
 * new artifact ids filled in. Attaching happens later, only if the draft
 * is actually saved (see {@code ConceptVersionWriter}).
 *
 * <p><b>Failsafe.</b> A picture is never allowed to fail the whole visual:
 * <ul>
 *   <li><b>image</b> - if the image cannot be produced and the payload
 *   carries a {@code fallbackDiagram} (the AI's own flowchart of the same
 *   idea), that diagram becomes the result. With no diagram to fall back
 *   on, the failure propagates and {@code VisualizePipeline} builds one
 *   from the concept text.</li>
 *   <li><b>animation</b> - a scene whose picture cannot be produced is
 *   kept without one (the frontend plays coded motion for it). After the
 *   first failure the remaining scene pictures are skipped rather than
 *   retrying a provider that is evidently down, scene after scene.</li>
 * </ul>
 */
@Component
public class VisualAssetGenerator {

    private static final Logger log = LoggerFactory.getLogger(VisualAssetGenerator.class);

    private final VisualGenerationProvider visualProvider;
    private final ArtifactStorage artifactStorage;

    public VisualAssetGenerator(VisualGenerationProvider visualProvider, ArtifactStorage artifactStorage) {
        this.visualProvider = visualProvider;
        this.artifactStorage = artifactStorage;
    }

    public Result generate(VisualizationPayload payload, UUID userId, UUID generationJobId) {
        if (payload instanceof ImagePayload image) {
            return generateImage(image, userId, generationJobId);
        }
        if (payload instanceof AnimationPayload animation) {
            return generateSceneImages(animation, userId, generationJobId);
        }
        // mind_map and diagram carry no generated visual assets today
        return new Result(payload, List.of());
    }

    private Result generateImage(ImagePayload image, UUID userId, UUID generationJobId) {
        try {
            Artifact artifact = generateAndUpload(image.imagePrompt(), userId, generationJobId);
            return new Result(
                    new ImagePayload(image.version(), image.imagePrompt(), artifact.getId().toString(),
                            image.altText(), image.fallbackDiagram()),
                    List.of(artifact.getId()));
        } catch (RuntimeException e) {
            if (image.fallbackDiagram() == null) {
                throw e;
            }
            log.warn("Job {}: image generation failed ({}), using the AI-written flowchart instead.",
                    generationJobId, e.getMessage());
            return new Result(image.fallbackDiagram(), List.of());
        }
    }

    private Result generateSceneImages(AnimationPayload animation, UUID userId, UUID generationJobId) {
        List<UUID> artifactIds = new ArrayList<>();
        List<AnimationPayload.Scene> updatedScenes = new ArrayList<>();
        boolean imagesAvailable = true;

        for (AnimationPayload.Scene scene : animation.scenes()) {
            boolean wantsImage = scene.needsVisualAsset() && scene.visualPrompt() != null && !scene.visualPrompt().isBlank();
            if (!wantsImage || !imagesAvailable) {
                updatedScenes.add(scene);
                continue;
            }
            try {
                Artifact artifact = generateAndUpload(scene.visualPrompt(), userId, generationJobId);
                artifactIds.add(artifact.getId());
                updatedScenes.add(new AnimationPayload.Scene(
                        scene.id(), scene.order(), scene.title(), scene.narration(),
                        scene.durationSeconds(), scene.transitionToNext(), scene.needsVisualAsset(),
                        scene.visualPrompt(), List.of(artifact.getId().toString())));
            } catch (RuntimeException e) {
                imagesAvailable = false;
                log.warn("Job {}: scene image generation failed ({}), remaining scenes will play without pictures.",
                        generationJobId, e.getMessage());
                updatedScenes.add(scene);
            }
        }
        return new Result(new AnimationPayload(animation.version(), updatedScenes), artifactIds);
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
