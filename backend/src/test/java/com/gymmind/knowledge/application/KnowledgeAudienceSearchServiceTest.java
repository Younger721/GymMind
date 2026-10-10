package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeAudienceSearchServiceTest {
    @Test
    void memberOnlyRetrievesDocumentsTargetingMemberRole() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var quotas = mock(TenantQuotaService.class);
        var repository = mock(KnowledgeDocumentRepository.class);
        var coachDocument = document(7L, 21L, Set.of(RoleCode.COACH));
        var memberDocument = document(7L, 22L, Set.of(RoleCode.MEMBER));
        when(keyword.search("q", 7L, 12L)).thenReturn(List.of(
                chunk("coach", coachDocument), chunk("member", memberDocument)));
        when(vector.search(any(), eq(7L), eq(12L))).thenReturn(List.of());
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(repository.findByTenantIdAndIdIn(eq(7L), any())).thenReturn(List.of(coachDocument, memberDocument));
        when(repository.findPlatformDocuments()).thenReturn(List.of());

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, quotas, repository);
        List<ContextSegment> result = service.search(new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of(), 0, "member"), "q");

        assertThat(result).extracting(ContextSegment::documentId).containsExactly("22");
    }

    @Test
    void platformAdminSearchesAllTenantScopes() {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var repository = mock(KnowledgeDocumentRepository.class);
        var first = document(7L, 31L, Set.of(RoleCode.MEMBER));
        var second = document(8L, 32L, Set.of(RoleCode.COACH));
        when(keyword.search(eq("q"), any(Long.class), eq(1L))).thenAnswer(invocation -> {
            Long tenant = invocation.getArgument(1);
            return tenant == 7L ? List.of(chunk("first", first)) : tenant == 8L ? List.of(chunk("second", second)) : List.of();
        });
        when(vector.search(any(), any(Long.class), eq(1L))).thenReturn(List.of());
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(repository.findByIdIn(any())).thenReturn(List.of(first, second));
        when(repository.findTenantIds()).thenReturn(List.of(7L, 8L));

        var service = new DefaultKnowledgeSearchService(keyword, vector, embedding, null, null, repository);
        List<ContextSegment> result = service.search(new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform"), "q");

        assertThat(result).extracting(ContextSegment::documentId).containsExactly("31", "32");
    }

    private static KnowledgeDocument document(long tenantId, long id, Set<RoleCode> roles) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, "doc-" + id + ".txt", "text/plain", DocumentVisibility.TENANT, id);
        document.assignId(id);
        document.updateAudienceRoles(roles);
        document.markParsing();
        document.markReady();
        document.publish();
        return document;
    }

    private static IndexedChunk chunk(String id, KnowledgeDocument document) {
        return new IndexedChunk(id, document.tenantId(), document.id(), document.ownerUserId(), id, new float[1024]);
    }
}
