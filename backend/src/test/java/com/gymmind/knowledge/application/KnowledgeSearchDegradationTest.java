package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchDegradationTest {

    @Test
    void returnsResultsWhenVectorPathFailsAndDualRecallNotRequired() {
        var keyword = mock(KeywordIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        var context = new KnowledgeSearchDegradationContext();
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of(
                new IndexedChunk("k1", 7L, "keyword hit", new float[1024])));
        when(embedding.embed(any())).thenThrow(new IllegalStateException("embedding down"));
        var events = new ArrayList<AiProgressEvent>();

        new DefaultKnowledgeSearchService(keyword, mock(VectorIndexPort.class), embedding, null, quotas,
                null, false, context, new SearchDegradationMetrics(new SimpleMeterRegistry()), null)
                .search(actor(), "q", events::add);

        assertThat(context.headerValue()).isEqualTo("vector");
        assertThat(events).anyMatch(event -> "recall_degraded".equals(event.stage())
                && event.status() == AiProgressEvent.Status.COMPLETED);
    }

    @Test
    void rejectsWhenDualRecallRequiredAndVectorPathFails() {
        var keyword = mock(KeywordIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        when(embedding.embed(any())).thenThrow(new IllegalStateException("embedding down"));

        assertThatThrownBy(() -> new DefaultKnowledgeSearchService(
                keyword, mock(VectorIndexPort.class), embedding, null, quotas,
                null, true, null, null, null).search(actor(), "q"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void rejectsWhenBothRecallPathsFail() {
        var keyword = mock(KeywordIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenThrow(new IllegalStateException("es down"));
        when(embedding.embed(any())).thenThrow(new IllegalStateException("embedding down"));

        assertThatThrownBy(() -> new DefaultKnowledgeSearchService(
                keyword, mock(VectorIndexPort.class), embedding, null, quotas)
                .search(actor(), "q"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
    }

    private static CurrentActor actor() {
        return new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:read"), 0, "t");
    }
}
