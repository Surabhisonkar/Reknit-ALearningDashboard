package com.learningdashboard.backend.storage;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.ConceptArtifactLinker;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter for the concept module's {@link ConceptArtifactLinker} port.
 * Unlike {@link ArtifactStorage#attachToConcept} (which trusts its
 * caller), this re-checks ownership per artifact - the ids come from a
 * stored draft, and a concept must never adopt another user's asset.
 * Not {@code @Transactional}: it always runs inside the caller's
 * transaction (confirm-save or the worker's regenerate).
 */
@Component
public class StorageConceptArtifactLinker implements ConceptArtifactLinker {

    private final ArtifactRepository artifactRepository;

    public StorageConceptArtifactLinker(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Override
    public void link(Collection<UUID> artifactIds, UUID userId, UUID conceptId, UUID conceptVersionId) {
        for (UUID artifactId : artifactIds) {
            Artifact artifact = artifactRepository.findByIdAndUserId(artifactId, userId)
                    .orElseThrow(() -> new NotFoundException("Artifact not found: " + artifactId));
            artifact.attachToConcept(conceptId, conceptVersionId); // clears orphan expiry
            artifactRepository.save(artifact);
        }
    }
}
