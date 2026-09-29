package com.learningdashboard.backend.concept;
import java.util.UUID;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirm-save: the one and only way a new concept comes into existence.
 * Promotes a draft (see {@link ConceptDraftSource}) into a {@link Concept}
 * plus its version-1 {@link ConceptVersion}. The duplicate-title check
 * runs <em>before</em> anything is written, so a {@link
 * DuplicateTitlePolicy#REJECT} collision leaves the database untouched.
 */
@Service
public class ConceptSaveService {

    static final int MAX_FOLDER_LENGTH = 120;
    static final int MAX_TITLE_LENGTH = 255;

    private final ConceptDraftSource draftSource;
    private final ConceptService conceptService;
    private final ConceptVersionWriter versionWriter;
    private final FolderAssigner folderAssigner;

    public ConceptSaveService(ConceptDraftSource draftSource, ConceptService conceptService,
                              ConceptVersionWriter versionWriter, FolderAssigner folderAssigner) {
        this.draftSource = draftSource;
        this.conceptService = conceptService;
        this.versionWriter = versionWriter;
        this.folderAssigner = folderAssigner;
    }

    @Transactional
    public SaveOutcome save(SaveConceptCommand command) {
        DraftClaim claim = draftSource.claim(command.draftId(), command.userId());

        if (claim.savedConceptId().isPresent()) {
            // Idempotent retry / double click: hand back what's already saved.
            return new SaveOutcome(conceptService.requireOwnedConcept(claim.savedConceptId().get(), command.userId()), false);
        }

        ConceptDraft draft = applyTitleOverride(claim.draft(), command.titleOverride());
        Optional<Concept> collision = conceptService.findTitleCollision(command.userId(), draft.title(), null);
        if (collision.isPresent() && command.onDuplicate() == DuplicateTitlePolicy.REJECT) {
            throw new DuplicateTitleException(collision.get().getId(), draft.title());
        }

        Concept concept = conceptService.save(new Concept(
                command.userId(), draft.title(), draft.summary(),
                resolveFolderId(command.userId(), draft, command.folderOverride()),
                draft.visualizationType(), draft.visualizationPayloadJson(), draft.payloadSchemaVersion(),
                draft.sourceExplainJobId()));
        concept = versionWriter.write(concept, 1, draft);

        if (collision.isPresent() && command.onDuplicate() == DuplicateTitlePolicy.REPLACE) {
            conceptService.deleteOwnedConcept(collision.get().getId(), command.userId());
        }

        draftSource.markSaved(command.draftId(), concept.getId());
        return new SaveOutcome(concept, true);
    }

    private ConceptDraft applyTitleOverride(ConceptDraft draft, String titleOverride) {
        if (titleOverride == null || titleOverride.isBlank()) {
            return draft;
        }
        return draft.withTitle(truncate(titleOverride.trim(), MAX_TITLE_LENGTH));
    }

    /**
     * Auto-files the new concept: the user's override wins, otherwise the
     * AI's suggestion. The folder is found case-insensitively or created,
     * inside this same transaction - so this runs only after the duplicate-
     * title check, and a failed save leaves no stray folder behind. A blank
     * name (override "" included) saves the concept unfiled.
     */
    private UUID resolveFolderId(UUID userId, ConceptDraft draft, String folderOverride) {
        String raw = folderOverride != null ? folderOverride : draft.suggestedFolder();
        String name = raw == null ? "" : truncate(raw.trim(), MAX_FOLDER_LENGTH);
        return name.isEmpty() ? null : folderAssigner.findOrCreate(userId, name).orElse(null);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

}
