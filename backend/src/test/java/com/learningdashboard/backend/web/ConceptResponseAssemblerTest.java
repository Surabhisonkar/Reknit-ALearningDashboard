package com.learningdashboard.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.folder.FolderColor;
import com.learningdashboard.backend.folder.FolderLabel;
import com.learningdashboard.backend.folder.FolderQueryService;
import com.learningdashboard.backend.web.dto.ConceptResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConceptResponseAssemblerTest {

    private final FolderQueryService folders = mock(FolderQueryService.class);
    private final ConceptResponseAssembler assembler =
            new ConceptResponseAssembler(folders, new ResponseMapper(new ObjectMapper()));

    private final UUID userId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();

    private Concept concept(UUID inFolder) {
        return new Concept(userId, "T", "s", inFolder, "mind_map", "{}", 1, null);
    }

    @Test
    void anUnfiledConceptKeepsTheOldEmptyFolderStringAndHasNoFolderIdOrColour() {
        ConceptResponse response = assembler.toResponse(concept(null));

        assertThat(response.folder()).isEmpty();
        assertThat(response.folderId()).isNull();
        assertThat(response.folderColor()).isNull();
        verify(folders, never()).labelFor(any(), any());
    }

    @Test
    void aFiledConceptCarriesItsFoldersNameIdAndColourKey() {
        when(folders.labelFor(userId, folderId)).thenReturn(Optional.of(new FolderLabel(folderId, "Biology", FolderColor.TEAL)));

        ConceptResponse response = assembler.toResponse(concept(folderId));

        assertThat(response.folder()).isEqualTo("Biology");
        assertThat(response.folderId()).isEqualTo(folderId);
        assertThat(response.folderColor()).isEqualTo("teal");
    }

    @Test
    void aListUsesOneFolderLookupForAllItsConcepts() {
        when(folders.labelsFor(userId)).thenReturn(Map.of(folderId, new FolderLabel(folderId, "Biology", FolderColor.TEAL)));

        List<ConceptResponse> responses = assembler.toResponses(userId, List.of(concept(folderId), concept(folderId), concept(null)));

        assertThat(responses).extracting(ConceptResponse::folder).containsExactly("Biology", "Biology", "");
        verify(folders, times(1)).labelsFor(userId);
    }
}
