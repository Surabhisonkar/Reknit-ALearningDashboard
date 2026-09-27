package com.learningdashboard.backend.generation.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learningdashboard.backend.common.exception.GenerationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderFallbackExecutorTest {

    private final ProviderFallbackExecutor executor = new ProviderFallbackExecutor();

    @Test
    void returnsFirstProviderResultWhenItSucceeds() {
        List<String> providers = List.of("first", "second");
        String result = executor.executeWithFallback(providers, p -> p + "-ok", "test");
        assertThat(result).isEqualTo("first-ok");
    }

    @Test
    void fallsThroughToNextProviderOnFailure() {
        List<String> providers = List.of("bad", "good");
        String result = executor.executeWithFallback(providers, p -> {
            if (p.equals("bad")) {
                throw new RuntimeException("simulated provider failure");
            }
            return p + "-ok";
        }, "test");
        assertThat(result).isEqualTo("good-ok");
    }

    @Test
    void triesEachProviderAtMostOnce() {
        List<String> providers = List.of("a", "b", "c");
        AtomicInteger callCount = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeWithFallback(providers, p -> {
            callCount.incrementAndGet();
            throw new RuntimeException("all fail");
        }, "test")).isInstanceOf(GenerationException.class);

        assertThat(callCount.get()).isEqualTo(3);
    }

    @Test
    void throwsGenerationExceptionWhenEveryProviderFails() {
        List<String> providers = List.of("a", "b");
        assertThatThrownBy(() -> executor.executeWithFallback(providers, p -> {
            throw new RuntimeException("simulated failure for " + p);
        }, "text generation"))
                .isInstanceOf(GenerationException.class)
                .extracting(e -> ((GenerationException) e).getCode())
                .isEqualTo(GenerationException.Code.ALL_PROVIDERS_FAILED);
    }

    @Test
    void throwsOnEmptyProviderList() {
        assertThatThrownBy(() -> executor.executeWithFallback(List.of(), p -> p, "test"))
                .isInstanceOf(IllegalStateException.class);
    }
}
