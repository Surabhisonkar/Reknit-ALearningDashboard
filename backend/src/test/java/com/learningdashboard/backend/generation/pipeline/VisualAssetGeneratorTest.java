package com.learningdashboard.backend.generation.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.generation.model.AnimationPayload;
import com.learningdashboard.backend.generation.model.DiagramPayload;
import com.learningdashboard.backend.generation.model.ImagePayload;
import com.learningdashboard.backend.generation.provider.VisualGenerationProvider;
import com.learningdashboard.backend.storage.Artifact;
import com.learningdashboard.backend.storage.ArtifactStorage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisualAssetGeneratorTest {

    private final VisualGenerationProvider visualProvider = mock(VisualGenerationProvider.class);
    private final ArtifactStorage storage = mock(ArtifactStorage.class);
    private final VisualAssetGenerator generator = new VisualAssetGenerator(visualProvider, storage);

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    private final DiagramPayload twin = new DiagramPayload(1,
            List.of(new DiagramPayload.Element("e1", "process", "Light hits leaf", 0, 0, 100, 50, "", "Light hits the leaf")),
            List.of());

    private UUID stubSuccessfulUpload() {
        UUID artifactId = UUID.randomUUID();
        Artifact artifact = mock(Artifact.class);
        when(artifact.getId()).thenReturn(artifactId);
        when(visualProvider.generateVisual(anyString()))
                .thenReturn(new VisualGenerationProvider.VisualAsset(new byte[] {1}, "image/png"));
        when(storage.uploadGenerated(any(), anyString(), any(), any())).thenReturn(artifact);
        return artifactId;
    }

    private static AnimationPayload.Scene scene(String id, int order, boolean needsImage) {
        return new AnimationPayload.Scene(id, order, "Title " + id, "Narration", 4, "fade",
                needsImage, needsImage ? "a prompt" : "", List.of());
    }

    @Test
    void imageSuccessKeepsTheDiagramTwinForTheToggle() {
        UUID artifactId = stubSuccessfulUpload();

        VisualAssetGenerator.Result result =
                generator.generate(new ImagePayload(1, "a leaf", "", "A leaf", twin), userId, jobId);

        ImagePayload image = (ImagePayload) result.payload();
        assertThat(image.artifactId()).isEqualTo(artifactId.toString());
        assertThat(image.fallbackDiagram()).isEqualTo(twin);
        assertThat(result.artifactIds()).containsExactly(artifactId);
    }

    @Test
    void imageFailureFallsBackToTheAiWrittenDiagram() {
        when(visualProvider.generateVisual(anyString())).thenThrow(new RuntimeException("quota exceeded"));

        VisualAssetGenerator.Result result =
                generator.generate(new ImagePayload(1, "a leaf", "", "A leaf", twin), userId, jobId);

        assertThat(result.payload()).isEqualTo(twin);
        assertThat(result.artifactIds()).isEmpty();
    }

    @Test
    void uploadFailureAlsoFallsBackToTheDiagram() {
        when(visualProvider.generateVisual(anyString()))
                .thenReturn(new VisualGenerationProvider.VisualAsset(new byte[] {1}, "image/png"));
        when(storage.uploadGenerated(any(), anyString(), any(), any())).thenThrow(new RuntimeException("s3 down"));

        assertThat(generator.generate(new ImagePayload(1, "a leaf", "", "A leaf", twin), userId, jobId).payload())
                .isEqualTo(twin);
    }

    @Test
    void imageFailureWithNoTwinPropagatesSoThePipelineCanBuildOne() {
        when(visualProvider.generateVisual(anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> generator.generate(new ImagePayload(1, "a leaf", "", "A leaf", null), userId, jobId))
                .isInstanceOf(RuntimeException.class).hasMessage("timeout");
    }

    @Test
    void sceneImageFailureKeepsEverySceneAndStopsAskingForPictures() {
        when(visualProvider.generateVisual(anyString())).thenThrow(new RuntimeException("503"));
        AnimationPayload animation = new AnimationPayload(1,
                List.of(scene("s1", 0, true), scene("s2", 1, false), scene("s3", 2, true)));

        VisualAssetGenerator.Result result = generator.generate(animation, userId, jobId);

        AnimationPayload out = (AnimationPayload) result.payload();
        assertThat(out.scenes()).hasSize(3);
        assertThat(out.scenes()).allSatisfy(s -> assertThat(s.assetArtifactIds()).isEmpty());
        assertThat(result.artifactIds()).isEmpty();
        verify(visualProvider, times(1)).generateVisual(anyString());
    }

    @Test
    void sceneImagesAreAttachedWhenTheProviderWorks() {
        UUID artifactId = stubSuccessfulUpload();
        AnimationPayload animation = new AnimationPayload(1, List.of(scene("s1", 0, true), scene("s2", 1, false)));

        AnimationPayload out = (AnimationPayload) generator.generate(animation, userId, jobId).payload();

        assertThat(out.scenes().get(0).assetArtifactIds()).containsExactly(artifactId.toString());
        assertThat(out.scenes().get(1).assetArtifactIds()).isEmpty();
    }
}
