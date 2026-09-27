package com.learningdashboard.backend.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    @Test
    void identicalVectorsHaveZeroDistance() {
        float[] a = {1f, 2f, 3f};
        float[] b = {1f, 2f, 3f};
        assertThat(CosineSimilarity.distance(a, b)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void oppositeVectorsHaveMaximalDistance() {
        float[] a = {1f, 0f};
        float[] b = {-1f, 0f};
        assertThat(CosineSimilarity.distance(a, b)).isCloseTo(2.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void orthogonalVectorsHaveMidpointDistance() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertThat(CosineSimilarity.distance(a, b)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void zeroVectorIsTreatedAsMaximallyDissimilar() {
        float[] a = {0f, 0f};
        float[] b = {1f, 1f};
        assertThat(CosineSimilarity.distance(a, b)).isEqualTo(1.0);
    }

    @Test
    void mismatchedDimensionsThrow() {
        float[] a = {1f, 2f};
        float[] b = {1f, 2f, 3f};
        assertThatThrownBy(() -> CosineSimilarity.distance(a, b)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void closerVectorsHaveSmallerDistanceThanFartherOnes() {
        float[] query = {1f, 1f, 0f};
        float[] close = {1f, 0.9f, 0.1f};
        float[] far = {-1f, -1f, 0f};

        double closeDistance = CosineSimilarity.distance(query, close);
        double farDistance = CosineSimilarity.distance(query, far);

        assertThat(closeDistance).isLessThan(farDistance);
    }
}
