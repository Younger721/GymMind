package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
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
    void exposesOnlyAuthorizedRecallDetailsAndCapsEachExcerpt() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        var documents = mock(KnowledgeDocumentRepository.class);
        var allowed = readyDocument(7L, 11L, "allowed.txt");
        var blocked = readyDocument(7L, 12L, "blocked.txt");
        blocked.updateAudienceRoles(Set.of(RoleCode.MEMBER));
        when(documents.findByTenantIdAndIdIn(eq(7L), any())).thenReturn(List.of(allowed, blocked));
        when(documents.findPlatformDocuments()).thenReturn(List.of());
        String longText = "x".repeat(400);
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of(
                new IndexedChunk("allowed-chunk", 7L, 11L, null, longText, new float[1024]),
                new IndexedChunk("blocked-chunk", 7L, 12L, null, "不应通过权限筛选", new float[1024])));
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        var events = new ArrayList<AiProgressEvent>();

        new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas, documents)
                .search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "t"), "q", events::add);

        AiProgressEvent keywordDone = events.stream()
                .filter(event -> "keyword_recall".equals(event.stage()) && event.status() == AiProgressEvent.Status.COMPLETED)
                .findFirst().orElseThrow();
        assertThat(keywordDone.resultCount()).isEqualTo(1);
        assertThat(keywordDone.details()).hasSize(1);
        assertThat(keywordDone.details().get(0).documentId()).isEqualTo("11");
        assertThat(keywordDone.details().get(0).excerpt()).hasSize(300);
        assertThat(keywordDone.details()).allMatch(detail -> detail.excerpt().length() <= 300);
    }

    private static KnowledgeDocument readyDocument(long tenantId, long id, String fileName) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, fileName, "text/plain",
                DocumentVisibility.TENANT, String.valueOf(id));
        document.assignId(id);
        document.markParsing();
        document.markReady();
        document.publish();
        return document;
    }

    @Test
    void fusionKeepsAuthorizedChunksWhenIndexStillReturnsDeletedDocumentNoise() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        var documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeDocument handbook = readyDocument(1L, 50L, "健身房知识汇总手册.docx");
        handbook.assignIndexGeneration("gen-50");
        when(documents.findByTenantIdAndIdIn(eq(1L), any())).thenReturn(List.of(handbook));
        when(documents.findPlatformDocuments()).thenReturn(List.of());
        String query = "深蹲应该怎么做";
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        IndexedChunk good = new IndexedChunk("good-squat", 1L, 50L, null,
                "深蹲动作规范：两脚与肩同宽，背挺直，膝盖与脚尖方向一致。", new float[1024])
                .withIndexGeneration("gen-50");
        List<IndexedChunk> staleVectorHits = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            staleVectorHits.add(new IndexedChunk("stale-" + i, 1L, 99L, null,
                    "已删除文档里的深蹲噪声片段 " + i, new float[1024]).withIndexGeneration("gen-deleted"));
        }
        when(keyword.search(eq(query), eq(1L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(List.of(good));
        when(vector.search(any(), eq(1L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class)))
                .thenReturn(staleVectorHits);
        var events = new ArrayList<AiProgressEvent>();

        var segments = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas, documents)
                .search(new CurrentActor(2L, 1L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "t"), query, events::add);

        AiProgressEvent fusion = events.stream()
                .filter(event -> "fusion".equals(event.stage()) && event.status() == AiProgressEvent.Status.COMPLETED)
                .findFirst().orElseThrow();
        assertThat(fusion.resultCount()).isGreaterThan(0);
        assertThat(segments).extracting(com.gymmind.ai.domain.ContextSegment::id).contains("good-squat");
    }

    @Test
    void reportsPermissionRerankAndCitationStagesAroundHybridRecall() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of(
                new IndexedChunk("k1", 7L, "keyword", new float[1024])));
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(vector.search(any(), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        var events = new ArrayList<AiProgressEvent>();

        new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas)
                .search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "t"), "q", events::add);

        assertThat(events).extracting(AiProgressEvent::stage)
                .containsSubsequence("permission_filter", "keyword_recall", "vector_recall",
                        "fusion", "rerank", "citation_prepare");
    }

    @Test
    void doesNotEmitCompletedZeroAfterVectorFailure() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        when(keyword.search(eq("q"), eq(7L), any(com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter.class))).thenReturn(List.of());
        when(embedding.embed(any())).thenThrow(new IllegalStateException("embedding unavailable"));
        var events = new ArrayList<AiProgressEvent>();

        new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas)
                .search(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "t"), "q", events::add);

        assertThat(events).filteredOn(event -> "vector_recall".equals(event.stage()))
                .extracting(AiProgressEvent::status)
                .containsExactly(AiProgressEvent.Status.RUNNING, AiProgressEvent.Status.FAILED);
    }
}
