package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchRelevanceTest {
    @Test
    void removesAChunkThatOnlySharesGenericTrainingWords() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        String query = "新手做力量训练要注意什么？";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of(
                new IndexedChunk("common", 7L, "有氧训练用于提升心肺能力与热量消耗，建议安排在力量训练之后，或单独安排在低强度日。", new float[1024]),
                new IndexedChunk("answer", 7L, "训练安全\n新手进行力量训练时应先热身，使用稳定重量并注意安全。", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .containsExactly("answer");
    }
}
