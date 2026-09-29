package com.learningdashboard.backend.web;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.folder.FolderLabel;
import com.learningdashboard.backend.folder.FolderQueryService;
import com.learningdashboard.backend.web.dto.ConceptResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds concept responses with their folder's name and colour. A concept
 * only stores its folder id, so this is where the two modules' data meet -
 * in the web layer, keeping controllers thin and the modules independent.
 */
@Component
public class ConceptResponseAssembler {

    private final FolderQueryService folderQueryService;
    private final ResponseMapper responseMapper;

    public ConceptResponseAssembler(FolderQueryService folderQueryService, ResponseMapper responseMapper) {
        this.folderQueryService = folderQueryService;
        this.responseMapper = responseMapper;
    }

    public ConceptResponse toResponse(Concept concept) {
        FolderLabel label = concept.getFolderId() == null
                ? null
                : folderQueryService.labelFor(concept.getUserId(), concept.getFolderId()).orElse(null);
        return responseMapper.toConceptResponse(concept, label);
    }

    /** One folder query for the whole list, however many concepts it has. */
    public List<ConceptResponse> toResponses(UUID userId, List<Concept> concepts) {
        if (concepts.isEmpty()) {
            return List.of();
        }
        Map<UUID, FolderLabel> labels = folderQueryService.labelsFor(userId);
        return concepts.stream()
                .map(concept -> responseMapper.toConceptResponse(
                        concept, concept.getFolderId() == null ? null : labels.get(concept.getFolderId())))
                .toList();
    }
}
