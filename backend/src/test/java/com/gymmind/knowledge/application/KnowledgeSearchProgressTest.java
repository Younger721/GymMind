package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchProgressTest {

    @Test
    void reportsPermissionRerankAndCitationStagesAroundHybridRecall() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        when(keyword.search("q", 7L, 1L)).thenReturn(List.of(
                new IndexedChunk("k1", 7L, "keyword", new float[1024])));
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());
        var events = new ArrayList<AiProgressEvent>();

        new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas)
                .search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "t"), "q", events::add);

        assertThat(events).extracting(AiProgressEvent::stage)
                .containsSubsequence("permission_filter", "keyword_recall", "vector_recall",
                        "fusion", "rerank", "citation_prepare");
    }
}
