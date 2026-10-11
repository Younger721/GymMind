package com.gymmind.knowledge.infrastructure.search;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryEmbeddingCacheTest {

    @Test
    void reusesEmbeddingForSameQuery() {
        var cache = new SearchQueryEmbeddingCache(60, 10);
        AtomicInteger calls = new AtomicInteger();
        float[] first = cache.getOrCompute("  hello  ", () -> {
            calls.incrementAndGet();
            return new float[] {1f, 2f};
        });
        float[] second = cache.getOrCompute("hello", () -> {
            calls.incrementAndGet();
            return new float[] {9f};
        });
        assertThat(first).containsExactly(1f, 2f);
        assertThat(second).isSameAs(first);
        assertThat(calls).hasValue(1);
    }
}
