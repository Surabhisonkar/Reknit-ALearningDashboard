package com.learningdashboard.backend.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VectorJsonConverterTest {

    private final VectorJsonConverter converter = new VectorJsonConverter();

    @Test
    void roundTripsAVectorThroughJson() {
        float[] original = {0.1f, -0.2f, 3.5f, 0f};

        String json = converter.convertToDatabaseColumn(original);
        float[] restored = converter.convertToEntityAttribute(json);

        assertThat(restored).containsExactly(original);
    }

    @Test
    void serializesAsAFlatJsonArray() {
        String json = converter.convertToDatabaseColumn(new float[]{1f, 2f, 3f});
        assertThat(json).isEqualTo("[1.0,2.0,3.0]");
    }

    @Test
    void handlesEmptyVector() {
        float[] restored = converter.convertToEntityAttribute("[]");
        assertThat(restored).isEmpty();
    }

    @Test
    void nullInNullOut() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
