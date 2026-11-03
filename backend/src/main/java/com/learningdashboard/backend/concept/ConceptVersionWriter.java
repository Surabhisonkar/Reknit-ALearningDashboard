package com.learningdashboard.backend.concept;

import org.springframework.stereotype.Component;

/**
 * The single code path that writes one version of a concept - shared by
 * confirm-save (version 1 of a brand-new concept) and post-save
 * regenerate (version N+1 of an existing one), so the two can never
 * drift apart. In order:
 * <ol>
 *   <li>insert the {@code concept_versions} row (final payload - assets were already generated and uploaded as orphans)</li>
 *   <li>attach the draft's artifacts to (concept, version), clearing their orphan expiry</li>
 *   <li>refresh the concept's denormalized current-version cache</li>
 *   <li>request re-indexing of the concept's embedding</li>
 * </ol>
 * Must run inside the caller's transaction. Deliberately not
 * {@code @Transactional} itself - see {@link
 * ConceptService#requireOwnedConceptForUpdate} for why.
 */
@Component
public class ConceptVersionWriter {

    private final ConceptService conceptService;
    private final ConceptArtifactLinker artifactLinker;
    private final ConceptIndexer conceptIndexer;

    public ConceptVersionWriter(ConceptService conceptService, ConceptArtifactLinker artifactLinker,
                                ConceptIndexer conceptIndexer) {
        this.conceptService = conceptService;
        this.artifactLinker = artifactLinker;
        this.conceptIndexer = conceptIndexer;
    }

    /**
     * @param concept an already-persisted concept (it must have a row for the version's FK)
     * @return the concept as saved, with its cache pointing at {@code versionNumber}
     */
    public Concept write(Concept concept, int versionNumber, ConceptDraft draft) {
        return write(concept, versionNumber, draft, true);
    }

    /**
     * @param reindex false skips step 4, for a version whose title and summary
     *                didn't change (a user's own edit of the visual), so saving
     *                it costs no embedding call
     */
    public Concept write(Concept concept, int versionNumber, ConceptDraft draft, boolean reindex) {
        ConceptVersion version = conceptService.saveVersion(new ConceptVersion(
                concept.getId(), versionNumber, draft.title(), draft.summary(),
                draft.visualizationType(), draft.visualizationPayloadJson(), draft.sourceText()));

        if (!draft.artifactIds().isEmpty()) {
            artifactLinker.link(draft.artifactIds(), concept.getUserId(), concept.getId(), version.getId());
        }

        concept.applyNewVersion(versionNumber, draft.title(), draft.summary(), draft.visualizationType(),
                draft.visualizationPayloadJson(), draft.payloadSchemaVersion());
        Concept saved = conceptService.save(concept);

        if (reindex) {
            conceptIndexer.requestIndexing(saved.getId(), saved.getUserId());
        }
        return saved;
    }
}
