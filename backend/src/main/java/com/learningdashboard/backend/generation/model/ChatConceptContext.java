package com.learningdashboard.backend.generation.model;

/** What the chat prompts know about the concept - deliberately not the Concept entity, so pipelines stay decoupled. */
public record ChatConceptContext(String title, String summary, String visualizationType) { }
