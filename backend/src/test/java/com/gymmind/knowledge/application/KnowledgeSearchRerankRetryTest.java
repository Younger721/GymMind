package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.domain.RankedChunk;
import com.gymmind.ai.domain.RerankGateway;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchRerankRetryTest {

    @Test
    void retriesRerankBeforeLocalFallback() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var rerank = mock(RerankGateway.class);
        var quotas = mock(TenantQuotaService.class);
        AtomicInteger attempts = new AtomicInteger();
        when(keyword.search(eq("q"), eq(7L), any(KnowledgeRecallFilter.class))).thenReturn(List.of(
                new IndexedChunk("c1", 7L, "alpha beta gamma", new float[8])));
        when(vector.search(any(), eq(7L), any(KnowledgeRecallFilter.class))).thenReturn(List.of());
        when(embedding.embed(any())).thenReturn(List.of(new float[8]));
        when(rerank.rerank(eq("q"), any())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("transient");
            }
            return List.of(new RankedChunk("c1", 1));
        });

        var props = new KnowledgeSearchProperties(
                30, 20, 3, 12000,
                false, 60, 10,
                2, 0,
                2, 4, 6, 4);
        var service = new DefaultKnowledgeSearchService(
                keyword, vector, embedding, rerank, quotas, null, false, null, null, props);
        List<ContextSegment> result = service.search(
                new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of(), 0, "member"), "q");

        assertThat(attempts).hasValue(2);
        assertThat(result).extracting(ContextSegment::id).containsExactly("c1");
    }
}
