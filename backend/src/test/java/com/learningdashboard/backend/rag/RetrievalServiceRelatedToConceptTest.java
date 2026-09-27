package com.learningdashboard.backend.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptRepository;
import com.learningdashboard.backend.generation.provider.EmbeddingProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetrievalServiceRelatedToConceptTest {

    private final EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
    private final ConceptEmbeddingRepository embeddingRepository = mock(ConceptEmbeddingRepository.class);
    private final ConceptRepository conceptRepository = mock(ConceptRepository.class);
    private final RetrievalService service = new RetrievalService(embeddingProvider, embeddingRepository, conceptRepository);

    private final UUID userId = UUID.randomUUID();
    private final Concept self = concept("Back muscles");
    private final Concept posture = concept("Posture");            // distance ~0.006 from self
    private final Concept core = concept("Core stability");        // distance 0.4
    private final Concept sourdough = concept("Sourdough");        // distance 1.0

    private Concept concept(String title) {
        return new Concept(userId, title, title + " summary", "", "mind_map", "{}", 1, null);
    }

    @BeforeEach
    void wiring() {
        ConceptEmbedding selfVec = new ConceptEmbedding(self.getId(), userId, new float[] {1f, 0f});
        when(embeddingRepository.findByConceptId(self.getId())).thenReturn(Optional.of(selfVec));
        when(embeddingRepository.findByUserId(userId)).thenReturn(List.of(
                selfVec,
                new ConceptEmbedding(posture.getId(), userId, new float[] {0.9f, 0.1f}),
                new ConceptEmbedding(core.getId(), userId, new float[] {0.6f, 0.8f}),
                new ConceptEmbedding(sourdough.getId(), userId, new float[] {0f, 1f}),
                new ConceptEmbedding(UUID.randomUUID(), userId, new float[] {1f, 0f, 0f}))); // other model's dimension

        Map<UUID, Concept> byId = Map.of(self.getId(), self, posture.getId(), posture, core.getId(), core,
                sourdough.getId(), sourdough);
        when(conceptRepository.findAllById(anyIterable())).thenAnswer(inv -> {
            Iterable<UUID> ids = inv.getArgument(0);
            return StreamSupport.stream(ids.spliterator(), false).map(byId::get).filter(c -> c != null).toList();
        });
    }

    @Test
    void keepsOnlyCloseConceptsExcludingItselfWithoutCallingTheProvider() {
        List<RelatedConcept> related = service.findRelatedToConcept(self.getId(), userId, 5, 0.35);

        assertThat(related).extracting(RelatedConcept::title).containsExactly("Posture");
        verifyNoInteractions(embeddingProvider);
    }

    @Test
    void ordersNearestFirstAndCapsAtTopK() {
        List<RelatedConcept> related = service.findRelatedToConcept(self.getId(), userId, 2, 2.0);

        assertThat(related).extracting(RelatedConcept::title).containsExactly("Posture", "Core stability");
        assertThat(related.get(0).distance()).isLessThan(related.get(1).distance());
    }

    @Test
    void skipsVectorsOfADifferentDimensionInsteadOfFailing() {
        List<RelatedConcept> related = service.findRelatedToConcept(self.getId(), userId, 10, 2.0);

        assertThat(related).hasSize(3); // posture, core, sourdough - the 3-dim vector is ignored
    }

    @Test
    void notYetIndexedConceptHasNoRelatedAndNeverFallsBackToTheProvider() {
        UUID unindexed = UUID.randomUUID();
        when(embeddingRepository.findByConceptId(unindexed)).thenReturn(Optional.empty());

        assertThat(service.findRelatedToConcept(unindexed, userId, 5, 0.35)).isEmpty();
        verify(embeddingRepository, never()).findByUserId(any());
        verifyNoInteractions(embeddingProvider);
    }

    @Test
    void anEmbeddingOwnedBySomeoneElseIsNeverUsedAsTheQuery() {
        UUID otherUser = UUID.randomUUID();
        assertThat(service.findRelatedToConcept(self.getId(), otherUser, 5, 2.0)).isEmpty();
    }
}
