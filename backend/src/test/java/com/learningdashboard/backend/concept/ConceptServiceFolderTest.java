package com.learningdashboard.backend.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.config.SparkMixProperties;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConceptServiceFolderTest {

    private final ConceptRepository repository = mock(ConceptRepository.class);
    private final FolderLookup folderLookup = mock(FolderLookup.class);
    private final ConceptService service =
            new ConceptService(repository, mock(ConceptVersionRepository.class), folderLookup,
                    new SparkFeedSampler(new SparkMixProperties()));

    private final UUID userId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();

    private Concept concept(UUID inFolder) {
        return new Concept(userId, "Photosynthesis", "s", inFolder, "animation", "{}", 1, null);
    }

    @Test
    void listingByAnUnknownFolderNameReturnsNothingLikeTheOldStringFilter() {
        when(folderLookup.findIdByName(userId, "Nope")).thenReturn(Optional.empty());

        assertThat(service.listForUser(userId, "Nope")).isEmpty();
        verify(repository, never()).findByUserIdAndFolderIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listingByAFolderNameFiltersByItsId() {
        Concept filed = concept(folderId);
        when(folderLookup.findIdByName(userId, "biology")).thenReturn(Optional.of(folderId));
        when(repository.findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId)).thenReturn(List.of(filed));

        assertThat(service.listForUser(userId, "biology")).containsExactly(filed);
    }

    @Test
    void aBlankFolderNameListsEverything() {
        service.listForUser(userId, "  ");
        verify(repository).findByUserIdOrderByCreatedAtDesc(userId);
        verify(folderLookup, never()).findIdByName(any(), any());
    }

    @Test
    void sparkFeedByAnUnknownFolderIsEmpty() {
        when(folderLookup.findIdByName(userId, "Nope")).thenReturn(Optional.empty());
        assertThat(service.randomSparkFeed(userId, "Nope", Set.of(), 10)).isEmpty();
    }

    @Test
    void sparkFeedServesEveryVisualTypeAndSkipsWhatTheClientHasSeen() {
        Concept animation = new Concept(userId, "A", "s", null, "animation", "{}", 1, null);
        Concept mindMap = new Concept(userId, "M", "s", null, "mind_map", "{}", 1, null);
        Concept image = new Concept(userId, "I", "s", null, "image", "{}", 1, null);
        Concept diagram = new Concept(userId, "D", "s", null, "diagram", "{}", 1, null);
        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(animation, mindMap, image, diagram));

        List<Concept> page = service.randomSparkFeed(userId, null, Set.of(diagram.getId()), 10);

        assertThat(page).containsExactlyInAnyOrder(animation, mindMap, image);
    }

    @Test
    void movingIntoSomeoneElsesFolderIsNotFoundAndChangesNothing() {
        Concept c = concept(null);
        when(repository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(folderLookup.isOwnedBy(folderId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.moveOwnedConcept(c.getId(), userId, folderId))
                .isInstanceOf(NotFoundException.class);
        assertThat(c.getFolderId()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void movingIntoAnOwnedFolderAndBackToUnfiled() {
        Concept c = concept(null);
        when(repository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(repository.save(c)).thenReturn(c);
        when(folderLookup.isOwnedBy(folderId, userId)).thenReturn(true);

        assertThat(service.moveOwnedConcept(c.getId(), userId, folderId).getFolderId()).isEqualTo(folderId);
        assertThat(service.moveOwnedConcept(c.getId(), userId, null).getFolderId()).isNull();
    }

    @Test
    void deleteAllInFolderDeletesOnlyThatFoldersConcepts() {
        List<Concept> inFolder = List.of(concept(folderId), concept(folderId));
        when(repository.findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId)).thenReturn(inFolder);

        assertThat(service.deleteAllInFolder(userId, folderId)).isEqualTo(2);
        verify(repository).deleteAll(inFolder);
    }
}
