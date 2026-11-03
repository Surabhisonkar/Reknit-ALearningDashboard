package com.learningdashboard.backend.generation.edit;

import java.util.List;

/** A user-edited visualization that can't be saved; {@code details} lists what was wrong (maps to 400). */
public class InvalidVisualizationEditException extends RuntimeException {

    private final List<String> details;

    public InvalidVisualizationEditException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() { return details; }
}
