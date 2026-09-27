package com.learningdashboard.backend.generation.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Small shared helper for reading/writing a job's JSON columns - one place for the "not valid JSON" error handling. */
@Component
public class JobJson {

    private final ObjectMapper objectMapper;

    public JobJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new GenerationException("Job payload was not valid JSON.", GenerationException.Code.INVALID_JSON);
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new GenerationException("Failed to serialize job result.", GenerationException.Code.INVALID_JSON, e);
        }
    }

    public ObjectMapper mapper() {
        return objectMapper;
    }

    /** Null-safe optional UUID field read. */
    public static UUID optionalUuid(JsonNode node, String field) {
        return node.hasNonNull(field) ? UUID.fromString(node.path(field).asText()) : null;
    }
}
