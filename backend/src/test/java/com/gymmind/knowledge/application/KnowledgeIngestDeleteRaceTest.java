package com.gymmind.knowledge.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.JobStatus;
import com.gymmind.shared.async.PersistentJob;
import com.gymmind.shared.async.PersistentJobQueue;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class KnowledgeIngestDeleteRaceTest {
    private static final Instant NOW = Instant.parse("2026-09-21T00:00:00Z");
    private static final CurrentActor ADMIN = new CurrentActor(11L, 7L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("knowledge:write"), 0, "t1");

    @Test
    void ingestFinishingAfterDeleteCannotRestoreDocumentOrLeaveItsChunksIndexed() {
        SnapshotKnowledgeDocumentRepository documents = new SnapshotKnowledgeDocumentRepository();
        KnowledgeDocument original = KnowledgeDocument.create(7L, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "stable-key");
        original.assignId(3L);
        documents.save(original);

        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeObjectStore store = mock(KnowledgeObjectStore.class);
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        when(jobs.claimNext(NOW, Duration.ofMinutes(5))).thenReturn(Optional.of(new PersistentJob(
                99L, KnowledgeIngestWorker.JOB_TYPE + ":3", 7L, JobStatus.RUNNING, 0,
                NOW, NOW.plusSeconds(300), null)));
        when(store.read(original.objectKey())).thenReturn("tenant guide".getBytes());

        DefaultKnowledgeDocumentUseCase useCase = new DefaultKnowledgeDocumentUseCase(
                documents, store, indexing, null, null);
        when(indexing.publish(eq(7L), eq(3L), isNull(), eq("guide.txt"), eq("text/plain"), any(byte[].class)))
                .thenAnswer(invocation -> {
                    useCase.delete(ADMIN, 3L);
                    return true;
                });

        new KnowledgeIngestWorker(jobs, documents, store, indexing, Clock.fixed(NOW, ZoneOffset.UTC))
                .processNext();

        assertThat(documents.findByTenantIdAndId(7L, 3L).orElseThrow().status())
                .isEqualTo(DocumentStatus.DELETED);
        verify(indexing, times(2)).deleteDocument(7L, 3L);
        verify(jobs).complete(99L);
        verify(jobs, never()).retry(eq(99L), any(), any());
    }
}
