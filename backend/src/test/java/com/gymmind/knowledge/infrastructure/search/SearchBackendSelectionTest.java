package com.gymmind.knowledge.infrastructure.search;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class SearchBackendSelectionTest {
    @Test
    void memoryIndexesUseTheSameBackendPropertiesAsRealIndexes() {
        var keyword = InMemoryKeywordIndex.class.getAnnotation(ConditionalOnProperty.class);
        var vector = InMemoryVectorIndex.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(keyword.name()).containsExactly("gymmind.search.keyword-backend");
        assertThat(keyword.havingValue()).isEqualTo("memory");
        assertThat(keyword.matchIfMissing()).isFalse();
        assertThat(vector.name()).containsExactly("gymmind.search.vector-backend");
        assertThat(vector.havingValue()).isEqualTo("memory");
        assertThat(vector.matchIfMissing()).isFalse();
    }
}
