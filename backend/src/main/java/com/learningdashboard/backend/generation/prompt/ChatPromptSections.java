package com.learningdashboard.backend.generation.prompt;

import com.learningdashboard.backend.generation.model.ChatConceptContext;
import com.learningdashboard.backend.generation.model.ChatMessage;
import java.util.List;

/** The <concept> and <conversation> blocks both chat prompts share, with every user-supplied value neutralized. */
final class ChatPromptSections {

    private ChatPromptSections() { }

    static void appendConcept(StringBuilder sb, ChatConceptContext concept) {
        sb.append("<concept>\n");
        if (concept != null) {
            sb.append("title: ").append(PromptText.neutralizeTags(concept.title())).append('\n');
            sb.append("summary: ").append(PromptText.neutralizeTags(concept.summary())).append('\n');
            sb.append("shown as: ").append(PromptText.neutralizeTags(concept.visualizationType())).append('\n');
        }
        sb.append("</concept>");
    }

    static void appendConversation(StringBuilder sb, List<ChatMessage> history) {
        if (history.isEmpty()) {
            return;
        }
        sb.append("\n\n<conversation>\n");
        for (ChatMessage message : history) {
            sb.append(message.fromUser() ? "User: " : "Tutor: ")
                    .append(PromptText.neutralizeTags(message.text()))
                    .append('\n');
        }
        sb.append("</conversation>");
    }
}
