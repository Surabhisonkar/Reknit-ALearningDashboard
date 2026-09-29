package com.learningdashboard.backend.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeastUsedFolderColorAssignerTest {

    private final FolderRepository repository = mock(FolderRepository.class);
    private final LeastUsedFolderColorAssigner assigner = new LeastUsedFolderColorAssigner(repository);
    private final UUID userId = UUID.randomUUID();

    @Test
    void theFirstFolderGetsTheFirstPaletteColour() {
        when(repository.findColorsByUserId(userId)).thenReturn(List.of());
        assertThat(assigner.nextColor(userId)).isEqualTo(FolderColor.CORAL);
    }

    @Test
    void picksTheLeastUsedColourWithTiesInPaletteOrder() {
        when(repository.findColorsByUserId(userId))
                .thenReturn(List.of(FolderColor.CORAL, FolderColor.YELLOW, FolderColor.SKY));
        assertThat(assigner.nextColor(userId)).isEqualTo(FolderColor.TEAL);
    }

    @Test
    void onceEveryColourIsUsedItStartsTheSecondRoundAtTheLeastUsed() {
        when(repository.findColorsByUserId(userId)).thenReturn(List.of(FolderColor.values()));
        assertThat(assigner.nextColor(userId)).isEqualTo(FolderColor.CORAL);
    }
}
