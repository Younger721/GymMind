package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.domain.RankedChunk;
import com.gymmind.ai.domain.RerankGateway;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Test
    void keepsFaqChunkForLongBeginnerCardioQuestionWhenLexicalThresholdIsStrict() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        String query = "新手应该是先练有氧还是先练力量？";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(
                        new IndexedChunk("generic", 7L, "有氧训练用于提升心肺能力，建议安排在力量训练之后。", new float[1024]),
                        new IndexedChunk("faq", 7L,
                                "问：新手应该从有氧还是力量开始？\n答：建议以基础力量动作为主，搭配少量低强度有氧。", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .contains("faq");
    }

    @Test
    void dropsComplianceHandbookWhenItDoesNotMatchQuestionTerms() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        String query = "新手应该先练有氧还是先练力量？";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(
                        new IndexedChunk("handbook", 7L,
                                "平台管理员上传 Word 后等待 READY 再发布；未发布文档不参与 AI 检索。", new float[1024]),
                        new IndexedChunk("faq", 7L,
                                "问：新手应该从有氧还是力量开始？\n答：建议以基础力量动作为主，搭配少量低强度有氧。", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .containsExactly("faq");
    }

    @Test
    void howToSquatPrefersActionChunkWhenRerankReturnsOnlyFaq() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        RerankGateway rerank = mock(RerankGateway.class);
        String query = "深蹲应该怎么做";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), anyLong(), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(
                        new IndexedChunk("faq", 7L,
                                "问：新手应该从有氧还是力量开始？\n答：建议以基础力量动作为主，先掌握深蹲、硬拉等。", new float[1024]),
                        new IndexedChunk("squat", 7L,
                                "深蹲动作规范\n动作要点\n1. 屈髋屈膝同时下蹲，膝盖与脚尖方向一致。", new float[1024])));
        when(vector.search(any(), anyLong(), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of());
        when(rerank.rerank(eq(query), any())).thenReturn(List.of(new RankedChunk("faq", 1)));

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, rerank, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .contains("squat")
                .doesNotContain("faq");
    }

    @Test
    void externalRerankOrderPrecedesLocalLexicalScore() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        RerankGateway rerank = mock(RerankGateway.class);
        String query = "深蹲训练会员";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(
                        new IndexedChunk("local-high", 7L, "深蹲训练会员动作规范", new float[1024]),
                        new IndexedChunk("rerank-first", 7L, "深蹲训练安全说明", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());
        when(rerank.rerank(eq(query), any())).thenReturn(List.of(new RankedChunk("rerank-first", 1)));

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, rerank, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .containsSubsequence("rerank-first", "local-high");
    }

    @Test
    void externalRerankOrderRemainsPrimaryAcrossTenantAndPlatformCandidates() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        RerankGateway rerank = mock(RerankGateway.class);
        String query = "深蹲训练会员";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(KnowledgeIndexingService.PLATFORM_INDEX_TENANT_ID),
                any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(new IndexedChunk("rerank-first", KnowledgeIndexingService.PLATFORM_INDEX_TENANT_ID,
                        "深蹲训练安全说明", new float[1024])));
        when(keyword.search(eq(query), eq(7L),
                any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(new IndexedChunk("tenant-local-high", 7L,
                        "深蹲训练会员动作规范", new float[1024])));
        when(vector.search(any(), eq(KnowledgeIndexingService.PLATFORM_INDEX_TENANT_ID),
                any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        when(vector.search(any(), eq(7L),
                any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        when(rerank.rerank(eq(query), any())).thenReturn(List.of(new RankedChunk("rerank-first", 1)));

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, rerank, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .containsSubsequence("rerank-first", "tenant-local-high");
    }

    @Test
    void sneakerQuestionRequiresChunkWithBothTrainingAreaAndSneakers() {
        KeywordIndexPort keyword = mock(KeywordIndexPort.class);
        VectorIndexPort vector = mock(VectorIndexPort.class);
        EmbeddingGateway embedding = mock(EmbeddingGateway.class);
        String query = "训练区对运动鞋有什么要求？";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(eq(query), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(
                        new IndexedChunk("ops", 7L,
                                "训练区须张贴消防疏散图，应急设备在有效期内；事件上报见 PLAT-OPS-001。", new float[1024]),
                        new IndexedChunk("safe", 7L,
                                "EVAL-PLAT-SAFE-001：进入训练区须穿包脚运动鞋，禁止拖鞋、赤足或使用露趾鞋。", new float[1024])));
        when(vector.search(any(), eq(7L), eq(1L))).thenReturn(List.of());

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, null);

        assertThat(service.search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"), query))
                .extracting(ContextSegment::id)
                .containsExactly("safe");
    }
}
