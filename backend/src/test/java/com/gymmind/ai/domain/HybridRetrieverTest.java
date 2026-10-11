package com.gymmind.ai.domain;

import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.SearchQueryEmbeddingCache;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.ai.harness.AiProgressEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

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
    void marksVectorDegradationWhenEmbeddingFails() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of(new IndexedChunk("k1", 7L, "keyword", new float[1024])));
        when(embedding.embed(any())).thenThrow(new IllegalStateException("down"));

        var result = new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20);

        assertThat(result.available()).isTrue();
        assertThat(result.degradationHeaderValue()).isEqualTo("vector");
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

    @Test
    void returnsAvailableEmptyResultWhenIndexesAreHealthyButHaveNoMatch() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of());
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var result = new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20);

        assertThat(result.available()).isTrue();
        assertThat(result.segments()).isEmpty();
    }

    @Test
    void reportsVectorFailureInsteadOfACompletedZeroResult() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of());
        when(embedding.embed(any())).thenThrow(new IllegalStateException("embedding unavailable"));
        var statuses = new ArrayList<AiProgressEvent.Status>();

        new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20,
                event -> {
                    if ("vector_recall".equals(event.stage())) statuses.add(event.status());
                });

        assertThat(statuses).containsExactly(AiProgressEvent.Status.RUNNING, AiProgressEvent.Status.FAILED);
    }

    @Test
    void reportsKeywordVectorAndFusionStages() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of(new IndexedChunk("k1", 7L, "keyword", new float[1024])));
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of(new IndexedChunk("v1", 7L, "vector", new float[1024])));
        var events = new ArrayList<String>();

        new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20,
                event -> events.add(event.stage()));

        assertThat(events).containsSubsequence("keyword_recall", "vector_recall", "fusion");
    }

    @Test
    void cachesQueryEmbeddingAcrossRetrieves() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var cache = new SearchQueryEmbeddingCache(60, 10);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of());
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());
        var retriever = new HybridRetriever(keyword, vector, embedding, cache);

        retriever.retrieve("q", 7L, 1L, 30, 20);
        retriever.retrieve("q", 7L, 1L, 30, 20);

        org.mockito.Mockito.verify(embedding, org.mockito.Mockito.times(1)).embed(any());
    }

    @Test
    void keepsRrfRelevanceOrderAfterFusion() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of(
                new IndexedChunk("zzz-relevant", 7L, "relevant", new float[1024]),
                new IndexedChunk("aaa-weak", 7L, "weak", new float[1024])));
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var result = new HybridRetriever(keyword, vector, embedding).retrieve("q", 7L, 1L, 30, 20);

        assertThat(result.segments()).extracting(ContextSegment::id)
                .containsExactly("zzz-relevant", "aaa-weak");
    }
}
