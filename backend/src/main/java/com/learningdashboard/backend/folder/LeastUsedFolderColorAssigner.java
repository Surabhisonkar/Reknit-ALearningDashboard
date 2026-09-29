package com.learningdashboard.backend.folder;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Picks the palette colour the user's folders use least, ties broken by
 * palette order - so the first eight folders all get different colours and
 * colour keeps meaning "topic" rather than repeating early.
 */
@Component
public class LeastUsedFolderColorAssigner implements FolderColorAssigner {

    private final FolderRepository folderRepository;

    public LeastUsedFolderColorAssigner(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Override
    public FolderColor nextColor(UUID userId) {
        Map<FolderColor, Integer> uses = new EnumMap<>(FolderColor.class);
        for (FolderColor color : FolderColor.values()) {
            uses.put(color, 0);
        }
        for (FolderColor used : folderRepository.findColorsByUserId(userId)) {
            uses.merge(used, 1, Integer::sum);
        }
        FolderColor best = FolderColor.values()[0];
        for (FolderColor color : FolderColor.values()) {
            if (uses.get(color) < uses.get(best)) {
                best = color;
            }
        }
        return best;
    }
}
