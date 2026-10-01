package com.learningdashboard.backend.generation.job;

import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.generation.model.ChatConceptContext;

/** Maps a concept to the little the chat prompts need to know about it. */
final class ChatContexts {

    private ChatContexts() { }

    static ChatConceptContext of(Concept concept) {
        return new ChatConceptContext(concept.getTitle(), concept.getSummary(), concept.getVisualizationType());
    }
}
