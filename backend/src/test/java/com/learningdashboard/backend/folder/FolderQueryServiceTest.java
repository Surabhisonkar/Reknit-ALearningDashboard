package com.learningdashboard.backend.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.concept.ConceptFolderStats;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FolderQueryServiceTest {

    private final FolderRepository repository = mock(FolderRepository.class);
    private final ConceptFolderStats stats = mock(ConceptFolderStats.class);
    private final FolderQueryService service = new FolderQueryService(repository, stats);

    private final UUID userId = UUID.randomUUID();
    private final Folder biology = new Folder(userId, "Biology", FolderColor.TEAL);
    private final Folder history = new Folder(userId, "History", FolderColor.SKY);

    @Test
    void listIncludesEmptyFoldersWithAZeroCount() {
        when(repository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(biology, history));
        when(stats.countByFolder(userId)).thenReturn(Map.of(biology.getId(), 3L));

        assertThat(service.listForUser(userId))
                .extracting(FolderSummary::conceptCount)
                .containsExactly(3L, 0L);
    }

    @Test
    void theLegacyNameListOnlyHasFoldersThatHoldConcepts() {
        when(repository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(biology, history));
        when(stats.countByFolder(userId)).thenReturn(Map.of(biology.getId(), 1L));

        assertThat(service.namesWithConcepts(userId)).containsExactly("Biology");
    }

    @Test
    void findIdByNameTrimsAndIgnoresBlankNames() {
        when(repository.findByUserIdAndName(userId, "Biology")).thenReturn(Optional.of(biology));

        assertThat(service.findIdByName(userId, "  Biology ")).contains(biology.getId());
        assertThat(service.findIdByName(userId, "  ")).isEmpty();
        verify(repository, times(1)).findByUserIdAndName(any(), any()); // only for the non-blank name
    }

    @Test
    void labelsCarryNameAndColour() {
        when(repository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(biology));

        FolderLabel label = service.labelsFor(userId).get(biology.getId());

        assertThat(label.name()).isEqualTo("Biology");
        assertThat(label.color()).isEqualTo(FolderColor.TEAL);
    }
}
