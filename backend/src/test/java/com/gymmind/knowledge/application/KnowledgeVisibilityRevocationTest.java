package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeChunk;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.IndexGenerationService;
import com.gymmind.knowledge.infrastructure.search.InMemoryKeywordIndex;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KnowledgeVisibilityRevocationTest {
    private static final CurrentActor ADMIN = new CurrentActor(11L, 7L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("knowledge:write"), 0, "t1");

    @Test
    void tighteningVisibilityRemovesTenantWideChunksBeforeQueuedReindexRuns() {
        SnapshotKnowledgeDocumentRepository documents = new SnapshotKnowledgeDocumentRepository();
        KnowledgeDocument original = KnowledgeDocument.create(7L, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "stable-key");
        original.assignId(3L);
        original.markParsing();
        original.markReady();
        documents.save(original);

        InMemoryKeywordIndex keyword = new InMemoryKeywordIndex();
        keyword.upsert(new com.gymmind.knowledge.infrastructure.search.IndexedChunk(
                "old-chunk", 7L, null, "confidential training details", null));
        KnowledgeChunkRepository chunks = mock(KnowledgeChunkRepository.class);
        when(chunks.findByTenantIdAndDocumentId(7L, 3L)).thenReturn(List.of(
                KnowledgeChunk.create(7L, 3L, "old-chunk", 1L, "confidential training details")));
        KnowledgeIndexingService indexing = new KnowledgeIndexingService(
                new IndexGenerationService(keyword, mock(VectorIndexPort.class)),
                mock(EmbeddingGateway.class), chunks);
        KnowledgeIngestWorker worker = mock(KnowledgeIngestWorker.class);
        DefaultKnowledgeDocumentUseCase useCase = new DefaultKnowledgeDocumentUseCase(
                documents, mock(KnowledgeObjectStore.class), indexing, worker, null);

        assertThat(keyword.search("confidential", 7L, 12L)).hasSize(1);

        assertThat(useCase.update(ADMIN, 3L,
                new UpdateKnowledgeDocumentCommand(DocumentVisibility.PRIVATE_USER)).status())
                .isEqualTo(DocumentStatus.READY);

        assertThat(keyword.search("confidential", 7L, 12L)).isEmpty();
        verify(worker).enqueue(7L, 3L);
        verify(chunks).deleteByTenantIdAndDocumentId(7L, 3L);
    }
}
