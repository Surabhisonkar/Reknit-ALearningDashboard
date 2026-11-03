package com.learningdashboard.backend.web.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

/** Body of {@code PUT /api/concepts/{id}/visualization}: the edited visualization, in the same JSON shape the API returns. */
public class VisualizationEditRequest {

    @NotNull(message = "visualization is required")
    private JsonNode visualization;

    public JsonNode getVisualization() { return visualization; }
    public void setVisualization(JsonNode visualization) { this.visualization = visualization; }
}
