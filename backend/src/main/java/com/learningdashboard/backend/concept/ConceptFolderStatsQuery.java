package com.learningdashboard.backend.concept;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** {@link ConceptFolderStats} backed by one GROUP BY over the user's concepts. */
@Component
public class ConceptFolderStatsQuery implements ConceptFolderStats {

    private final ConceptRepository conceptRepository;

    public ConceptFolderStatsQuery(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countByFolder(UUID userId) {
        List<Object[]> rows = conceptRepository.countByFolderIdForUser(userId);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
