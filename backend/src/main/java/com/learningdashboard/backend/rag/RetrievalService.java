package com.learningdashboard.backend.rag;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptRepository;
import com.learningdashboard.backend.generation.provider.EmbeddingProvider;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read side of server-side RAG. This is what the old {@code
 * cosineSimilarity.js}/{@code LocalEmbeddingStore} did in the browser
 * against whatever happened to be in that one browser's localStorage;
 * here it runs server-side against every concept the user has ever
 * saved, scoped by {@code userId} so retrieval never crosses tenants.
 *
 * <p>Postgres+pgvector would run this as a single indexed {@code
 * ORDER BY embedding <=> ? LIMIT k} query; MySQL has no equivalent
 * vector operator, so this loads the user's embeddings and ranks them
 * in the application layer instead (see {@link
 * ConceptEmbeddingRepository#findByUserId} and {@link CosineSimilarity}).
 * That's a real, documented scaling trade-off — fine for the per-user
 * concept counts a personal learning app expects (hundreds to low
 * thousands), but if a user's library grows large enough for this to
 * matter, this class's public method is the entire contract a future
 * OpenSearch-backed implementation would need to satisfy; nothing that
 * calls {@link #findRelated} would need to change.
 */
@Service
public class RetrievalService {

    private final EmbeddingProvider embeddingProvider;
    private final ConceptEmbeddingRepository embeddingRepository;
    private final ConceptRepository conceptRepository;

    public RetrievalService(EmbeddingProvider embeddingProvider, ConceptEmbeddingRepository embeddingRepository,
                             ConceptRepository conceptRepository) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingRepository = embeddingRepository;
        this.conceptRepository = conceptRepository;
    }

    /** Embeds {@code queryText} and returns the top-k most similar concepts already owned by {@code userId}. */
    public List<RelatedConcept> findRelated(String queryText, UUID userId, int topK) {
        List<ConceptEmbedding> candidates = embeddingRepository.findByUserId(userId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingProvider.embed(queryText);

        List<UUID> conceptIds = candidates.stream().map(ConceptEmbedding::getConceptId).toList();
        Map<UUID, Concept> conceptsById = new HashMap<>();
        conceptRepository.findAllById(conceptIds).forEach(c -> conceptsById.put(c.getId(), c));

        return candidates.stream()
                .map(embedding -> {
                    Concept concept = conceptsById.get(embedding.getConceptId());
                    if (concept == null) {
                        return null; // stale embedding row for a since-deleted concept - shouldn't happen (FK cascade), skip defensively
                    }
                    double distance = CosineSimilarity.distance(queryVector, embedding.getEmbedding());
                    return new RelatedConcept(concept.getId(), concept.getTitle(), concept.getSummary(), distance);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(RelatedConcept::distance))
                .limit(topK)
                .toList();
    }

    /**
     * The user-facing "Related concepts" read: concepts similar to one the
     * user already saved. Unlike {@link #findRelated}, this <b>never calls
     * the embedding provider</b> - the concept's stored embedding (already
     * {@code embed(title + ". " + summary)}, see EmbeddingIndexService) is
     * the query vector. That keeps a page view free of AI calls (the api
     * process never calls a provider), fast, and immune to provider
     * outages/quota.
     *
     * <ul>
     *   <li>No stored embedding yet (indexing still queued, or failed) - empty list, never a fallback provider call.</li>
     *   <li>The concept itself is excluded; so are vectors of a different
     *       dimension (from an embedding-model change), which are skipped
     *       rather than failing the request.</li>
     *   <li>Only candidates with {@code distance <= maxDistance} are kept, nearest first, at most {@code topK}.</li>
     *   <li>Folders are deliberately ignored - cross-folder links are the point.</li>
     * </ul>
     * Caller must have checked that {@code conceptId} belongs to {@code userId}.
     */
    public List<RelatedConcept> findRelatedToConcept(UUID conceptId, UUID userId, int topK, double maxDistance) {
        Optional<ConceptEmbedding> own = embeddingRepository.findByConceptId(conceptId)
                .filter(embedding -> userId.equals(embedding.getUserId()));
        if (own.isEmpty() || topK <= 0) {
            return List.of();
        }
        float[] queryVector = own.get().getEmbedding();

        List<ConceptEmbedding> candidates = embeddingRepository.findByUserId(userId).stream()
                .filter(embedding -> !conceptId.equals(embedding.getConceptId()))
                .filter(embedding -> embedding.getEmbedding() != null && embedding.getEmbedding().length == queryVector.length)
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        record Scored(UUID conceptId, double distance) { }
        List<Scored> nearest = candidates.stream()
                .map(embedding -> new Scored(embedding.getConceptId(), CosineSimilarity.distance(queryVector, embedding.getEmbedding())))
                .filter(scored -> scored.distance() <= maxDistance)
                .sorted(Comparator.comparingDouble(Scored::distance))
                .limit(topK)
                .toList();
        if (nearest.isEmpty()) {
            return List.of();
        }

        // Only the survivors' concept rows are loaded (titles/summaries), not every candidate's.
        Map<UUID, Concept> conceptsById = new HashMap<>();
        conceptRepository.findAllById(nearest.stream().map(Scored::conceptId).toList())
                .forEach(c -> conceptsById.put(c.getId(), c));

        return nearest.stream()
                .map(scored -> {
                    Concept concept = conceptsById.get(scored.conceptId());
                    return concept == null
                            ? null // embedding outlived its concept (shouldn't happen - FK cascade); skip defensively
                            : new RelatedConcept(concept.getId(), concept.getTitle(), concept.getSummary(), scored.distance());
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
