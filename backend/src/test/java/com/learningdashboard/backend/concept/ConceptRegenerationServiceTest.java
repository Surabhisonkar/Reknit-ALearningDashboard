package com.learningdashboard.backend.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConceptRegenerationServiceTest {

    private final ConceptService conceptService = mock(ConceptService.class);
    private final ConceptVersionWriter versionWriter = mock(ConceptVersionWriter.class);
    private final ConceptRegenerationService service = new ConceptRegenerationService(conceptService, versionWriter);

    @Test
    void appendsTheNextVersionNumber() {
        UUID userId = UUID.randomUUID();
        Concept concept = new Concept(userId, "Old", "s", "", "diagram", "{}", 1, null);
        concept.applyNewVersion(2, "Old", "s", "diagram", "{}", 1); // concept already has 2 versions
        ConceptDraft draft = new ConceptDraft(UUID.randomUUID(), null, "New", "s2", "", "diagram", "{}", 1, List.of());

        when(conceptService.requireOwnedConceptForUpdate(concept.getId(), userId)).thenReturn(concept);
        when(versionWriter.write(any(Concept.class), anyInt(), any(ConceptDraft.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conceptService.findTitleCollision(userId, "Old", concept.getId())).thenReturn(Optional.empty());

        VersionAppended appended = service.appendVersion(concept.getId(), userId, draft);

        assertThat(appended.version()).isEqualTo(3);
        assertThat(appended.conceptId()).isEqualTo(concept.getId());
        assertThat(appended.duplicateTitleConceptId()).isNull();
        verify(versionWriter).write(concept, 3, draft);
    }
}
