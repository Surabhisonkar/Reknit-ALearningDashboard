package com.learningdashboard.backend.storage;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.config.AwsProperties;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3ArtifactStorage implements ArtifactStorage {

    private static final Duration ORPHAN_TTL = Duration.ofHours(24);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ArtifactRepository artifactRepository;
    private final AwsProperties.S3 s3Properties;

    public S3ArtifactStorage(S3Client s3Client, S3Presigner s3Presigner,
                              ArtifactRepository artifactRepository, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.artifactRepository = artifactRepository;
        this.s3Properties = awsProperties.getS3();
    }

    @Override
    @Transactional
    public Artifact uploadGenerated(byte[] bytes, String contentType, UUID userId, UUID generationJobId) {
        requireBucketConfigured();
        String key = "generated/%s/%s.%s".formatted(userId, UUID.randomUUID(), extensionFor(contentType));

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.getArtifactsBucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));

        Artifact artifact = new Artifact(userId, generationJobId, null, key, contentType, bytes.length);
        artifact.markOrphanExpiry(Instant.now().plus(ORPHAN_TTL));
        return artifactRepository.save(artifact);
    }

    @Override
    @Transactional
    public Artifact attachToConcept(UUID artifactId, UUID conceptId, UUID conceptVersionId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new NotFoundException("Artifact not found: " + artifactId));
        artifact.attachToConcept(conceptId, conceptVersionId);
        return artifactRepository.save(artifact);
    }

    @Override
    public URL presignDownloadUrl(Artifact artifact) {
        requireBucketConfigured();
        return presignGet(artifact.getS3Key());
    }

    @Override
    public URL presignUploadUrl(String keyHint, String contentType, UUID userId) {
        requireBucketConfigured();
        String key = "uploads/%s/%s-%s".formatted(userId, UUID.randomUUID(), keyHint);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getArtifactsBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.getPresignTtlMinutes()))
                .putObjectRequest(putRequest)
                .build());

        return presigned.url();
    }

    private URL presignGet(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getArtifactsBucket())
                .key(key)
                .build();

        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.getPresignTtlMinutes()))
                .getObjectRequest(getRequest)
                .build());

        return presigned.url();
    }

    private String extensionFor(String contentType) {
        return contentType.contains("/") ? contentType.substring(contentType.indexOf('/') + 1) : "bin";
    }

    private void requireBucketConfigured() {
        if (s3Properties.getArtifactsBucket() == null || s3Properties.getArtifactsBucket().isBlank()) {
            throw new IllegalStateException("ARTIFACTS_BUCKET is not configured.");
        }
    }
}
