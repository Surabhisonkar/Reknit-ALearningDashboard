package com.learningdashboard.backend.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.concept.ConceptFolderContents;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

class FolderServiceTest {

    private final FolderRepository repository = mock(FolderRepository.class);
    private final FolderColorAssigner colorAssigner = mock(FolderColorAssigner.class);
    private final ConceptFolderContents contents = mock(ConceptFolderContents.class);
    private final FolderService service = new FolderService(repository, colorAssigner, contents);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void wiring() {
        when(repository.saveAndFlush(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(colorAssigner.nextColor(userId)).thenReturn(FolderColor.SKY);
    }

    @Test
    void createTrimsTheNameAndAutoAssignsAColourWhenNoneIsGiven() {
        Folder folder = service.createFolder(userId, "  Biology  ", null);
        assertThat(folder.getName()).isEqualTo("Biology");
        assertThat(folder.getColor()).isEqualTo(FolderColor.SKY);
    }

    @Test
    void createKeepsAnExplicitColour() {
        assertThat(service.createFolder(userId, "Biology", FolderColor.ROSE).getColor()).isEqualTo(FolderColor.ROSE);
        verify(colorAssigner, never()).nextColor(any());
    }

    @Test
    void createRejectsAnExistingNameBeforeWriting() {
        when(repository.existsByUserIdAndName(userId, "biology")).thenReturn(true);
        assertThatThrownBy(() -> service.createFolder(userId, "biology", null))
                .isInstanceOf(DuplicateFolderNameException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aConcurrentCreateThatHitsTheUniqueKeyIsStillADuplicateNotA500() {
        when(repository.saveAndFlush(any(Folder.class))).thenThrow(new DataIntegrityViolationException("uk_folders_user_name"));
        assertThatThrownBy(() -> service.createFolder(userId, "Biology", null))
                .isInstanceOf(DuplicateFolderNameException.class);
    }

    @Test
    void createRejectsABlankName() {
        assertThatThrownBy(() -> service.createFolder(userId, "   ", null))
                .isInstanceOf(InvalidFolderRequestException.class);
    }

    @Test
    void renameChecksDuplicatesExcludingItselfSoACaseOnlyRenameWorks() {
        Folder folder = new Folder(userId, "biology", FolderColor.TEAL);
        when(repository.findByIdAndUserId(folder.getId(), userId)).thenReturn(Optional.of(folder));

        Folder renamed = service.renameOrRecolor(folder.getId(), userId, "Biology", null);

        verify(repository).existsByUserIdAndNameAndIdNot(userId, "Biology", folder.getId());
        assertThat(renamed.getName()).isEqualTo("Biology");
        assertThat(renamed.getColor()).isEqualTo(FolderColor.TEAL);
    }

    @Test
    void renameToAnotherFoldersNameIsADuplicate() {
        Folder folder = new Folder(userId, "Biology", FolderColor.TEAL);
        when(repository.findByIdAndUserId(folder.getId(), userId)).thenReturn(Optional.of(folder));
        when(repository.existsByUserIdAndNameAndIdNot(userId, "Psychology", folder.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.renameOrRecolor(folder.getId(), userId, "Psychology", null))
                .isInstanceOf(DuplicateFolderNameException.class);
    }

    @Test
    void recolourAloneLeavesTheName() {
        Folder folder = new Folder(userId, "Biology", FolderColor.TEAL);
        when(repository.findByIdAndUserId(folder.getId(), userId)).thenReturn(Optional.of(folder));

        Folder updated = service.renameOrRecolor(folder.getId(), userId, null, FolderColor.GREEN);

        assertThat(updated.getName()).isEqualTo("Biology");
        assertThat(updated.getColor()).isEqualTo(FolderColor.GREEN);
    }

    @Test
    void someoneElsesFolderIsNotFound() {
        UUID folderId = UUID.randomUUID();
        when(repository.findByIdAndUserId(folderId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteOwnedFolder(folderId, userId, FolderDeletionMode.UNFILE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteInUnfileModeLeavesTheConceptsAlone() {
        Folder folder = new Folder(userId, "Biology", FolderColor.TEAL);
        when(repository.findByIdAndUserId(folder.getId(), userId)).thenReturn(Optional.of(folder));

        service.deleteOwnedFolder(folder.getId(), userId, FolderDeletionMode.UNFILE);

        verify(contents, never()).deleteAllInFolder(any(), any());
        verify(repository).delete(folder);
    }

    @Test
    void deleteWithConceptsDeletesThemFirstThenTheFolder() {
        Folder folder = new Folder(userId, "Biology", FolderColor.TEAL);
        when(repository.findByIdAndUserId(folder.getId(), userId)).thenReturn(Optional.of(folder));

        service.deleteOwnedFolder(folder.getId(), userId, FolderDeletionMode.DELETE_CONCEPTS);

        InOrder order = inOrder(contents, repository);
        order.verify(contents).deleteAllInFolder(userId, folder.getId());
        order.verify(repository).delete(folder);
    }

    @Test
    void findOrCreateReturnsTheExistingFolderWithoutInserting() {
        Folder existing = new Folder(userId, "Biology", FolderColor.TEAL);
        when(repository.lockByUserIdAndName(userId, "Biology")).thenReturn(Optional.of(existing));

        assertThat(service.findOrCreateByName(userId, " Biology ")).contains(existing);
        verify(repository, never()).insertIfAbsent(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void findOrCreateInsertsWithAnAutoColourThenReadsTheRowBack() {
        Folder created = new Folder(userId, "Anatomy", FolderColor.SKY);
        when(repository.lockByUserIdAndName(userId, "Anatomy")).thenReturn(Optional.empty(), Optional.of(created));

        assertThat(service.findOrCreateByName(userId, "Anatomy")).contains(created);
        verify(repository).insertIfAbsent(anyString(), eq(userId.toString()), eq("Anatomy"), eq("sky"), any());
    }

    @Test
    void findOrCreateIgnoresABlankName() {
        assertThat(service.findOrCreateByName(userId, "  ")).isEmpty();
        verify(repository, never()).lockByUserIdAndName(any(), any());
    }
}
