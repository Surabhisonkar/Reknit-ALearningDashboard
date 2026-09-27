package com.learningdashboard.backend.rag;

import com.learningdashboard.backend.generation.provider.EmbeddingProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write side of server-side RAG: embeds a concept's title+summary and
 * upserts it into {@code concept_embeddings}. Called by {@code
 * ConceptIndexJobHandler} (an async INDEX_CONCEPT job queued after a concept is saved or regenerated), so every saved
 * concept is retrievable as RAG context for future generations — this
 * never runs in the browser.
 */
@Service
public class EmbeddingIndexService {

    private final EmbeddingProvider embeddingProvider;
    private final ConceptEmbeddingRepository embeddingRepository;

    public EmbeddingIndexService(EmbeddingProvider embeddingProvider, ConceptEmbeddingRepository embeddingRepository) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingRepository = embeddingRepository;
    }

    /**
     * Called after every Visualize completion, including a regenerate -
     * a regenerate can produce a different title/summary each time, so
     * the RAG index has to stay current or future retrieval would keep
     * surfacing stale text for this concept. The 1:1 unique constraint on
     * concept_id means this is always an upsert: update the existing row
     * in place for a regenerate, insert a new one the first time.
     */
    @Transactional
    public void indexConcept(UUID conceptId, UUID userId, String title, String summary) {
        float[] vector = embeddingProvider.embed(title + ". " + summary);
        embeddingRepository.findByConceptId(conceptId).ifPresentOrElse(
                existing -> {
                    existing.updateEmbedding(vector);
                    embeddingRepository.save(existing);
                },
                () -> embeddingRepository.save(new ConceptEmbedding(conceptId, userId, vector))
        );
    }

    @Transactional
    public void removeIndex(UUID conceptId) {
        embeddingRepository.deleteByConceptId(conceptId);
    }
}
