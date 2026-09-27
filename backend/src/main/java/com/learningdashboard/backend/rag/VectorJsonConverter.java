package com.learningdashboard.backend.rag;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps a Java {@code float[]} onto a MySQL {@code json} column as a
 * plain JSON array of numbers, e.g. {@code [0.123,-0.456,...]}. Hand-
 * rolled rather than routed through Jackson's {@code ObjectMapper}: JPA
 * attribute converters are instantiated by the persistence provider,
 * not Spring, so pulling in a shared, Spring-managed {@code
 * ObjectMapper} here would need extra wiring for no real benefit — a
 * flat array of floats is about as simple a format as JSON gets, so a
 * small manual parser/writer avoids that wiring entirely while staying
 * just as correct.
 *
 * <p>This is what replaces the Postgres-only {@code pgvector} column
 * type from the earlier Postgres-based draft of this schema; see {@link
 * RetrievalService} for how similarity is computed now that there's no
 * database-side vector operator to lean on.
 */
@Converter
public class VectorJsonConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(attribute.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(attribute[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String trimmed = dbData.trim();
        String inner = trimmed.substring(trimmed.indexOf('[') + 1, trimmed.lastIndexOf(']'));
        if (inner.isBlank()) {
            return new float[0];
        }
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
