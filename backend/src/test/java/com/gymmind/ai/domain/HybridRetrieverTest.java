package com.gymmind.ai.domain;

import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridRetrieverTest {

    @Test
    void fusesKeywordAndVectorHits() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of(new IndexedChunk("k1", 7L, "keyword", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of(new IndexedChunk("v1", 7L, "vector", new float[1024])));

        var result = new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20);

        assertThat(result.available()).isTrue();
        assertThat(result.segments()).hasSize(2);
    }

    @Test
    void returnsUnavailableWhenBothPathsFail() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenThrow(new IllegalStateException("down"));
        when(embedding.embed(any())).thenThrow(new IllegalStateException("down"));

        var result = new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20);

        assertThat(result.available()).isFalse();
    }
}
