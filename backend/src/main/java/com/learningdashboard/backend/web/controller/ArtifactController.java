package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.config.AwsProperties;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.storage.Artifact;
import com.learningdashboard.backend.storage.ArtifactRepository;
import com.learningdashboard.backend.storage.ArtifactStorage;
import com.learningdashboard.backend.web.dto.ArtifactDownloadResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The only way the frontend ever gets at a generated image/asset: a
 * time-limited presigned URL, never a public or permanent one, and only
 * for artifacts the requesting user actually owns. Used by the mind_map
 * (none), diagram (none), animation (per-scene images), and image
 * renderer components on the frontend.
 */
@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    private final ArtifactRepository artifactRepository;
    private final ArtifactStorage artifactStorage;
    private final CurrentUserService currentUserService;
    private final AwsProperties.S3 s3Properties;

    public ArtifactController(ArtifactRepository artifactRepository, ArtifactStorage artifactStorage,
                               CurrentUserService currentUserService, AwsProperties awsProperties) {
        this.artifactRepository = artifactRepository;
        this.artifactStorage = artifactStorage;
        this.currentUserService = currentUserService;
        this.s3Properties = awsProperties.getS3();
    }

    @GetMapping("/{id}")
    public ArtifactDownloadResponse getDownloadUrl(@PathVariable UUID id) {
        var user = currentUserService.requireCurrentUser();
        Artifact artifact = artifactRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Artifact not found: " + id));

        String url = artifactStorage.presignDownloadUrl(artifact).toString();
        Instant expiresAt = Instant.now().plus(s3Properties.getPresignTtlMinutes(), ChronoUnit.MINUTES);
        return new ArtifactDownloadResponse(url, expiresAt);
    }
}
